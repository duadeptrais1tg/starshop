package com.starshop.service.impl;

import com.starshop.config.VnpayProperties;
import com.starshop.entity.Cart;
import com.starshop.entity.CartItem;
import com.starshop.entity.Order;
import com.starshop.entity.OrderItem;
import com.starshop.entity.Payment;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.entity.enums.PaymentMethod;
import com.starshop.entity.enums.PaymentStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.CartItemRepository;
import com.starshop.repository.CartRepository;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.PaymentRepository;
import com.starshop.service.CartService;
import com.starshop.service.OrderService;
import com.starshop.service.PaymentService;
import com.starshop.service.VnpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    /** Chờ thêm sau hạn thanh toán trước khi tự hủy (phòng IPN của VNPAY đến trễ). */
    static final int EXPIRE_GRACE_MINUTES = 5;
    /** Khách bấm hủy trên trang VNPAY. */
    static final String CUSTOMER_CANCELLED = "24";

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderService orderService;
    private final VnpayService vnpayService;
    private final VnpayProperties vnpayProperties;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public String createVnpayUrl(Long userId, String txnRef, String clientIp) {
        if (orderRepository.findByPaymentTxnRefAndUserIdOrderByIdAsc(txnRef, userId).isEmpty()) {
            throw new NotFoundException("Không tìm thấy giao dịch");
        }
        Payment payment = paymentRepository.findByTxnRef(txnRef)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy giao dịch"));
        if (payment.getMethod() != PaymentMethod.VNPAY || payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException("Giao dịch này đã được xử lý, không thể thanh toán lại.");
        }
        if (payment.getCreatedAt() != null
                && payment.getCreatedAt().plusMinutes(vnpayProperties.expireMinutes()).isBefore(LocalDateTime.now(clock))) {
            throw new BusinessException("Đã quá hạn thanh toán, đơn hàng sẽ được hủy tự động. Vui lòng đặt lại.");
        }
        return vnpayService.buildPaymentUrl(txnRef, payment.getAmount(), clientIp);
    }

    @Override
    @Transactional
    public VnpayCallbackResult handleVnpayCallback(Map<String, String> params) {
        if (!vnpayService.verify(params)) {
            log.warn("Callback VNPAY sai chữ ký: txnRef={}", params.get("vnp_TxnRef"));
            return new VnpayCallbackResult("97", "Invalid signature", null, false);
        }
        String txnRef = params.get("vnp_TxnRef");
        Payment payment = txnRef == null ? null : paymentRepository.findByTxnRefForUpdate(txnRef).orElse(null);
        if (payment == null || payment.getMethod() != PaymentMethod.VNPAY) {
            return new VnpayCallbackResult("01", "Order not found", null, false);
        }
        if (!sameAmount(payment.getAmount(), params.get("vnp_Amount"))) {
            log.warn("Callback VNPAY sai số tiền: txnRef={}, vnp_Amount={}", txnRef, params.get("vnp_Amount"));
            return new VnpayCallbackResult("04", "Invalid amount", txnRef, false);
        }
        // Đã xử lý (return và IPN cùng đến, hoặc VNPAY gửi lại IPN): không làm lại
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return new VnpayCallbackResult("02", "Order already confirmed", txnRef,
                    payment.getStatus() == PaymentStatus.PAID);
        }

        String responseCode = params.get("vnp_ResponseCode");
        boolean success = "00".equals(responseCode) && "00".equals(params.get("vnp_TransactionStatus"));
        if (success) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setTransactionNo(params.get("vnp_TransactionNo"));
            payment.setPaidAt(LocalDateTime.now(clock));
        } else {
            boolean cancelled = CUSTOMER_CANCELLED.equals(responseCode);
            fail(payment, cancelled ? PaymentStatus.CANCELLED : PaymentStatus.FAILED,
                    cancelled ? "Khách hủy thanh toán VNPAY" : "Thanh toán VNPAY không thành công (mã " + responseCode + ")");
        }
        return new VnpayCallbackResult("00", "Confirm Success", txnRef, success);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> staleVnpayTxnRefs() {
        LocalDateTime before = LocalDateTime.now(clock).minusMinutes(vnpayProperties.expireMinutes() + EXPIRE_GRACE_MINUTES);
        return paymentRepository.findStaleTxnRefs(PaymentMethod.VNPAY, PaymentStatus.PENDING, before);
    }

    @Override
    @Transactional
    public void expire(String txnRef) {
        paymentRepository.findByTxnRefForUpdate(txnRef)
                .filter(p -> p.getStatus() == PaymentStatus.PENDING)
                .ifPresent(p -> fail(p, PaymentStatus.FAILED, "Quá hạn thanh toán VNPAY"));
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Thanh toán không thành công: cập nhật Payment, trả sản phẩm lại giỏ, hủy đơn (OrderService hoàn tồn kho
     * và lượt mã). Thứ tự quan trọng: hủy đơn để cuối vì việc trả lượt mã sẽ clear persistence context.
     */
    private void fail(Payment payment, PaymentStatus status, String reason) {
        payment.setStatus(status);
        List<Order> orders = orderRepository.findByPaymentIdOrderByIdAsc(payment.getId());
        restoreCart(orders);
        List<Long> orderIds = orders.stream()
                .filter(o -> orderService.canTransition(o.getStatus(), OrderStatus.CANCELLED))
                .map(Order::getId)
                .toList();
        for (Long orderId : orderIds) {
            orderService.changeStatus(orderId, OrderStatus.CANCELLED, null, reason);
        }
    }

    /** Trả sản phẩm của các đơn không thanh toán được lại giỏ, để khách đặt lại nhanh. */
    private void restoreCart(List<Order> orders) {
        if (orders.isEmpty()) {
            return;
        }
        Order first = orders.get(0);
        Cart cart = cartRepository.findByUserId(first.getUser().getId())
                .orElseGet(() -> cartRepository.save(Cart.builder().user(first.getUser()).build()));
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                CartItem line = cartItemRepository.findByCartIdAndProductId(cart.getId(), item.getProduct().getId())
                        .orElse(null);
                if (line == null) {
                    cartItemRepository.save(CartItem.builder()
                            .cart(cart).product(item.getProduct()).quantity(item.getQuantity()).build());
                } else {
                    line.setQuantity(Math.min(line.getQuantity() + item.getQuantity(), CartService.MAX_QUANTITY_PER_ITEM));
                }
            }
        }
    }

    /** vnp_Amount = số tiền x 100. */
    private static boolean sameAmount(BigDecimal amount, String vnpAmount) {
        try {
            return vnpAmount != null && amount.movePointRight(2).compareTo(new BigDecimal(vnpAmount)) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
