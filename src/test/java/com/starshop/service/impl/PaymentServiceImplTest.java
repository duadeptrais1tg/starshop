package com.starshop.service.impl;

import com.starshop.config.VnpayProperties;
import com.starshop.entity.Cart;
import com.starshop.entity.CartItem;
import com.starshop.entity.Order;
import com.starshop.entity.OrderItem;
import com.starshop.entity.Payment;
import com.starshop.entity.Product;
import com.starshop.entity.User;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.entity.enums.PaymentMethod;
import com.starshop.entity.enums.PaymentStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.CartItemRepository;
import com.starshop.repository.CartRepository;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.PaymentRepository;
import com.starshop.service.OrderService;
import com.starshop.service.PaymentService.VnpayCallbackResult;
import com.starshop.service.VnpayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 5, 10, 0);

    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final CartRepository cartRepository = mock(CartRepository.class);
    private final CartItemRepository cartItemRepository = mock(CartItemRepository.class);
    private final OrderService orderService = mock(OrderService.class);
    private final VnpayService vnpayService = mock(VnpayService.class);
    private final PaymentServiceImpl service = new PaymentServiceImpl(paymentRepository, orderRepository, cartRepository,
            cartItemRepository, orderService, vnpayService,
            new VnpayProperties("TMN", "SECRET", "https://pay", "http://return", 15),
            Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));

    private Payment payment;
    private Order orderA;
    private Order orderB;
    private Cart cart;

    @BeforeEach
    void setUp() {
        payment = Payment.builder().id(9L).method(PaymentMethod.VNPAY).amount(new BigDecimal("830000.00"))
                .txnRef("PAY1").build();
        payment.setCreatedAt(NOW.minusMinutes(3));
        when(paymentRepository.findByTxnRefForUpdate("PAY1")).thenReturn(Optional.of(payment));
        when(paymentRepository.findByTxnRef("PAY1")).thenReturn(Optional.of(payment));
        when(vnpayService.verify(any())).thenReturn(true);

        User user = User.builder().id(7L).build();
        orderA = order(1L, user, OrderStatus.NEW, 10L, 2);
        orderB = order(2L, user, OrderStatus.NEW, 11L, 1);
        when(orderRepository.findByPaymentIdOrderByIdAsc(9L)).thenReturn(List.of(orderA, orderB));
        cart = Cart.builder().id(50L).user(user).build();
        when(cartRepository.findByUserId(7L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdAndProductId(anyLong(), anyLong())).thenReturn(Optional.empty());
        when(orderService.canTransition(OrderStatus.NEW, OrderStatus.CANCELLED)).thenReturn(true);
    }

    @Test
    void success_marksPaid_andKeepsOrders() {
        VnpayCallbackResult result = service.handleVnpayCallback(callback("00", "00", "83000000"));

        assertThat(result.rspCode()).isEqualTo("00");
        assertThat(result.paid()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(payment.getTransactionNo()).isEqualTo("14000001");
        assertThat(payment.getPaidAt()).isEqualTo(NOW);
        verify(orderService, never()).changeStatus(anyLong(), any(), any(), any());
    }

    @Test
    void duplicateCallback_isNotProcessedTwice() {
        service.handleVnpayCallback(callback("00", "00", "83000000"));
        // Lần 2 (IPN gửi lại / return đến sau IPN), kể cả khi nội dung khác
        VnpayCallbackResult again = service.handleVnpayCallback(callback("51", "02", "83000000"));

        assertThat(again.rspCode()).isEqualTo("02");
        assertThat(again.paid()).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verify(orderService, never()).changeStatus(anyLong(), any(), any(), any());
    }

    @Test
    void invalidSignature_amountOrTxn_areRejected_withoutChanges() {
        when(vnpayService.verify(any())).thenReturn(false);
        assertThat(service.handleVnpayCallback(callback("00", "00", "83000000")).rspCode()).isEqualTo("97");

        when(vnpayService.verify(any())).thenReturn(true);
        assertThat(service.handleVnpayCallback(callback("00", "00", "100")).rspCode()).isEqualTo("04");

        Map<String, String> unknown = callback("00", "00", "83000000");
        unknown.put("vnp_TxnRef", "KHONGCO");
        when(paymentRepository.findByTxnRefForUpdate("KHONGCO")).thenReturn(Optional.empty());
        assertThat(service.handleVnpayCallback(unknown).rspCode()).isEqualTo("01");

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void failedPayment_cancelsOrders_restoresCart() {
        VnpayCallbackResult result = service.handleVnpayCallback(callback("51", "02", "83000000"));

        assertThat(result.rspCode()).isEqualTo("00");
        assertThat(result.paid()).isFalse();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(orderService).changeStatus(eq(1L), eq(OrderStatus.CANCELLED), eq(null), anyString());
        verify(orderService).changeStatus(eq(2L), eq(OrderStatus.CANCELLED), eq(null), anyString());
        ArgumentCaptor<CartItem> restored = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemRepository, org.mockito.Mockito.times(2)).save(restored.capture());
        assertThat(restored.getAllValues()).extracting(i -> i.getProduct().getId()).containsExactly(10L, 11L);
        assertThat(restored.getAllValues()).extracting(CartItem::getQuantity).containsExactly(2, 1);
    }

    @Test
    void customerCancel_marksCancelled_andMergesIntoExistingCartLine() {
        CartItem existing = CartItem.builder().id(5L).cart(cart).product(orderA.getItems().get(0).getProduct()).quantity(3).build();
        when(cartItemRepository.findByCartIdAndProductId(50L, 10L)).thenReturn(Optional.of(existing));

        service.handleVnpayCallback(callback("24", "02", "83000000"));

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELLED);
        assertThat(existing.getQuantity()).isEqualTo(5);
        verify(orderService).changeStatus(eq(1L), eq(OrderStatus.CANCELLED), eq(null), eq("Khách hủy thanh toán VNPAY"));
    }

    @Test
    void expire_onlyPendingPayments() {
        service.expire("PAY1");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(orderService).changeStatus(eq(1L), eq(OrderStatus.CANCELLED), eq(null), eq("Quá hạn thanh toán VNPAY"));

        Payment paid = Payment.builder().id(3L).method(PaymentMethod.VNPAY).status(PaymentStatus.PAID).txnRef("PAY2").build();
        when(paymentRepository.findByTxnRefForUpdate("PAY2")).thenReturn(Optional.of(paid));
        service.expire("PAY2");
        assertThat(paid.getStatus()).isEqualTo(PaymentStatus.PAID);
    }

    @Test
    void staleTxnRefs_usesExpiryPlusGrace() {
        service.staleVnpayTxnRefs();
        verify(paymentRepository).findStaleTxnRefs(PaymentMethod.VNPAY, PaymentStatus.PENDING, NOW.minusMinutes(20));
    }

    @Test
    void createUrl_checksOwnerStatusAndExpiry() {
        when(orderRepository.findByPaymentTxnRefAndUserIdOrderByIdAsc("PAY1", 7L)).thenReturn(List.of(orderA));
        when(vnpayService.buildPaymentUrl("PAY1", payment.getAmount(), "1.1.1.1")).thenReturn("https://pay?x");

        assertThat(service.createVnpayUrl(7L, "PAY1", "1.1.1.1")).isEqualTo("https://pay?x");
        assertThatThrownBy(() -> service.createVnpayUrl(8L, "PAY1", "1.1.1.1")).isInstanceOf(NotFoundException.class);

        payment.setCreatedAt(NOW.minusMinutes(16));
        assertThatThrownBy(() -> service.createVnpayUrl(7L, "PAY1", "1.1.1.1")).hasMessageContaining("quá hạn");

        payment.setStatus(PaymentStatus.PAID);
        assertThatThrownBy(() -> service.createVnpayUrl(7L, "PAY1", "1.1.1.1")).isInstanceOf(BusinessException.class);
    }

    private static Map<String, String> callback(String responseCode, String transactionStatus, String amount) {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "PAY1");
        params.put("vnp_Amount", amount);
        params.put("vnp_ResponseCode", responseCode);
        params.put("vnp_TransactionStatus", transactionStatus);
        params.put("vnp_TransactionNo", "14000001");
        params.put("vnp_SecureHash", "abc");
        return params;
    }

    private static Order order(Long id, User user, OrderStatus status, Long productId, int qty) {
        Order order = Order.builder().id(id).user(user).status(status).build();
        order.addItem(OrderItem.builder().product(Product.builder().id(productId).build()).quantity(qty).build());
        return order;
    }
}
