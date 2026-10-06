package com.starshop.service.impl;

import com.starshop.config.OrderProperties;
import com.starshop.dto.UploadResult;
import com.starshop.dto.order.CancelReason;
import com.starshop.dto.order.UserOrderDetail;
import com.starshop.dto.order.UserOrderRow;
import com.starshop.dto.order.UserOrderTab;
import com.starshop.entity.Order;
import com.starshop.entity.OrderItem;
import com.starshop.entity.Payment;
import com.starshop.entity.ReturnRequest;
import com.starshop.entity.ShipperAssignment;
import com.starshop.entity.enums.MediaType;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.entity.enums.PaymentMethod;
import com.starshop.entity.enums.PaymentStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.OrderItemRepository;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.OrderStatusHistoryRepository;
import com.starshop.repository.ReturnRequestRepository;
import com.starshop.repository.ShipperAssignmentRepository;
import com.starshop.repository.spec.OrderSpecifications;
import com.starshop.service.FileStorageService;
import com.starshop.service.OrderService;
import com.starshop.service.UserOrderService;
import com.starshop.util.DateFormats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserOrderServiceImpl implements UserOrderService {

    private static final String RETURN_FOLDER = "returns";
    private static final int MIN_RETURN_REASON = 10;
    private static final int MAX_RETURN_REASON = 1000;
    private static final int MAX_OTHER_REASON = 300;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ShipperAssignmentRepository assignmentRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final OrderService orderService;
    private final FileStorageService fileStorageService;
    private final OrderProperties orderProperties;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Page<UserOrderRow> search(Long userId, UserOrderTab tab, int page) {
        UserOrderTab selected = tab == null ? UserOrderTab.NEW : tab;
        Page<Order> orders = orderRepository.findAll(
                Specification.allOf(OrderSpecifications.user(userId), OrderSpecifications.statusIn(selected.getStatuses())),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id")));

        // Sản phẩm của cả trang trong 1 query
        Map<Long, List<OrderItem>> itemsByOrder = new LinkedHashMap<>();
        if (orders.hasContent()) {
            for (OrderItem item : orderItemRepository.findByOrderIdInOrderByIdAsc(orders.map(Order::getId).getContent())) {
                itemsByOrder.computeIfAbsent(item.getOrder().getId(), k -> new ArrayList<>()).add(item);
            }
        }
        return orders.map(o -> {
            List<OrderItem> items = itemsByOrder.getOrDefault(o.getId(), List.of());
            OrderItem first = items.isEmpty() ? null : items.get(0);
            return UserOrderRow.builder()
                    .id(o.getId())
                    .code(o.getCode())
                    .createdAt(DateFormats.dateTime(o.getCreatedAt()))
                    .shopName(o.getShop().getName())
                    .shopSlug(o.getShop().getSlug())
                    .firstProductName(first == null ? null : first.getProductName())
                    .firstProductImage(first == null ? null : first.getProductImage())
                    .firstProductQuantity(first == null ? 0 : first.getQuantity())
                    .moreProducts(Math.max(items.size() - 1, 0))
                    .total(o.getTotal())
                    .status(o.getStatus())
                    .awaitingPayment(isAwaitingPayment(o))
                    .paymentTxnRef(o.getPayment() == null ? null : o.getPayment().getTxnRef())
                    .build();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UserOrderTab, Long> countByTab(Long userId) {
        Map<UserOrderTab, Long> counts = new EnumMap<>(UserOrderTab.class);
        for (UserOrderTab tab : UserOrderTab.values()) {
            counts.put(tab, orderRepository.count(Specification.allOf(
                    OrderSpecifications.user(userId), OrderSpecifications.statusIn(tab.getStatuses()))));
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public UserOrderDetail detail(Long userId, Long orderId) {
        Order o = requireOwnOrder(userId, orderId);
        Payment payment = o.getPayment();
        ShipperAssignment assignment = assignmentRepository.findFirstByOrderIdOrderByIdDesc(o.getId()).orElse(null);
        ReturnRequest returnRequest = returnRequestRepository.findByOrderId(o.getId()).orElse(null);
        LocalDateTime deadline = o.getDeliveredAt() == null ? null : o.getDeliveredAt().plusDays(orderProperties.returnDays());

        return UserOrderDetail.builder()
                .id(o.getId())
                .code(o.getCode())
                .createdAt(DateFormats.dateTime(o.getCreatedAt()))
                .status(o.getStatus())
                .shopName(o.getShop().getName())
                .shopSlug(o.getShop().getSlug())
                .receiverName(o.getReceiverName())
                .receiverPhone(o.getReceiverPhone())
                .shippingAddress(o.getShippingAddress())
                .note(o.getNote())
                .cancelReason(o.getCancelReason())
                .carrierName(o.getCarrier().getName())
                .shipperName(assignment == null ? null : assignment.getShipper().getFullName())
                .shipperPhone(assignment == null ? null : assignment.getShipper().getPhone())
                .deliveredAt(o.getDeliveredAt() == null ? null : DateFormats.dateTime(o.getDeliveredAt()))
                .paymentMethodLabel(o.getPaymentMethod().getLabel())
                .paymentStatusLabel(payment == null ? null : payment.getStatus().getLabel())
                .paid(payment != null && o.getPaymentMethod() != PaymentMethod.COD && payment.getStatus() == PaymentStatus.PAID)
                .awaitingPayment(isAwaitingPayment(o))
                .paymentTxnRef(payment == null ? null : payment.getTxnRef())
                .couponCode(o.getCoupon() == null ? null : o.getCoupon().getCode())
                .subtotal(o.getSubtotal())
                .productDiscount(o.getProductDiscount())
                .shippingFee(o.getShippingFee())
                .shippingDiscount(o.getShippingDiscount())
                .total(o.getTotal())
                .items(o.getItems().stream().map(i -> UserOrderDetail.Line.builder()
                        .productName(i.getProductName())
                        .productSlug(i.getProduct().getSlug())
                        .imageUrl(i.getProductImage())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .lineTotal(i.getLineTotal())
                        .build()).toList())
                .history(historyRepository.findByOrderIdOrderByCreatedAtAsc(o.getId()).stream()
                        .map(h -> UserOrderDetail.History.builder()
                                .time(DateFormats.dateTime(h.getCreatedAt()))
                                .toLabel(h.getToStatus().getLabel())
                                .note(h.getNote())
                                .build()).toList())
                .returnRequest(returnRequest == null ? null : UserOrderDetail.ReturnInfo.builder()
                        .reason(returnRequest.getReason())
                        .statusLabel(returnRequest.getStatus().getLabel())
                        .rejectReason(returnRequest.getRejectReason())
                        .imageUrls(List.copyOf(returnRequest.getImageUrls()))
                        .createdAt(DateFormats.dateTime(returnRequest.getCreatedAt()))
                        .build())
                .canCancel(o.getStatus() == OrderStatus.NEW && !isAwaitingPayment(o))
                .canRequestReturn(returnBlockedReason(o, returnRequest != null) == null)
                .returnDeadline(deadline == null ? null : DateFormats.dateTime(deadline))
                .returnDays(orderProperties.returnDays())
                .build();
    }

    @Override
    @Transactional
    public void cancel(Long userId, Long orderId, CancelReason reason, String otherReason) {
        Order order = requireOwnOrder(userId, orderId);
        if (reason == null) {
            throw new BusinessException("Vui lòng chọn lý do hủy đơn.");
        }
        String text = reason.getLabel();
        if (reason == CancelReason.OTHER) {
            if (!StringUtils.hasText(otherReason)) {
                throw new BusinessException("Vui lòng nhập lý do hủy đơn.");
            }
            text = otherReason.trim();
            if (text.length() > MAX_OTHER_REASON) {
                throw new BusinessException("Lý do tối đa " + MAX_OTHER_REASON + " ký tự.");
            }
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessException("Đơn " + order.getCode() + " đã được hủy trước đó.");
        }
        // Khách chỉ hủy khi shop chưa xác nhận (đơn đã xác nhận phải liên hệ shop)
        if (order.getStatus() != OrderStatus.NEW) {
            throw new BusinessException("Đơn " + order.getCode() + " đang ở trạng thái \"" + order.getStatus().getLabel()
                    + "\", không thể tự hủy. Vui lòng liên hệ shop.");
        }
        if (isAwaitingPayment(order)) {
            throw new BusinessException("Đơn đang chờ thanh toán VNPAY: bạn có thể hủy ngay trên trang thanh toán, "
                    + "hoặc để quá hạn đơn sẽ tự hủy.");
        }
        String note = "Khách hủy: " + text;
        if (order.getPaymentMethod() != PaymentMethod.COD && order.getPayment() != null
                && order.getPayment().getStatus() == PaymentStatus.PAID) {
            note += " (đã thanh toán " + order.getPaymentMethod().getLabel() + ", cần hoàn tiền)";
        }
        orderService.changeStatus(order.getId(), OrderStatus.CANCELLED, userId, note);
    }

    @Override
    @Transactional
    public void requestReturn(Long userId, Long orderId, String reason, List<MultipartFile> images) {
        Order order = requireOwnOrder(userId, orderId);
        String blocked = returnBlockedReason(order, returnRequestRepository.findByOrderId(orderId).isPresent());
        if (blocked != null) {
            throw new BusinessException(blocked);
        }
        String text = reason == null ? "" : reason.trim();
        if (text.length() < MIN_RETURN_REASON || text.length() > MAX_RETURN_REASON) {
            throw new BusinessException("Lý do trả hàng từ " + MIN_RETURN_REASON + " đến " + MAX_RETURN_REASON + " ký tự.");
        }
        List<MultipartFile> files = images == null ? List.of()
                : images.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (files.isEmpty()) {
            throw new BusinessException("Vui lòng gửi ít nhất 1 ảnh minh chứng.");
        }
        if (files.size() > MAX_RETURN_IMAGES) {
            throw new BusinessException("Tối đa " + MAX_RETURN_IMAGES + " ảnh.");
        }

        List<String> urls = new ArrayList<>();
        List<String> publicIds = new ArrayList<>();
        try {
            for (MultipartFile file : files) {
                UploadResult uploaded = fileStorageService.uploadImage(file, RETURN_FOLDER);
                urls.add(uploaded.url());
                publicIds.add(uploaded.publicId());
            }
        } catch (RuntimeException e) {
            publicIds.forEach(this::deleteQuietly);
            throw e;
        }
        deleteOnRollback(publicIds);

        returnRequestRepository.save(ReturnRequest.builder().order(order).reason(text).imageUrls(urls).build());
        orderService.changeStatus(order.getId(), OrderStatus.RETURN_REQUESTED, userId, "Khách yêu cầu trả hàng");
    }

    // ------------------------------------------------------------------ helpers

    /** Đơn phải là của chính user; đơn người khác coi như không tồn tại. */
    private Order requireOwnOrder(Long userId, Long orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
    }

    /** Lý do không yêu cầu trả hàng được; null = được. */
    private String returnBlockedReason(Order order, boolean alreadyRequested) {
        if (order.getStatus() != OrderStatus.DELIVERED) {
            return "Chỉ yêu cầu trả hàng khi đơn đã giao.";
        }
        if (alreadyRequested) {
            return "Đơn này đã có yêu cầu trả hàng, mỗi đơn chỉ được yêu cầu một lần.";
        }
        if (order.getDeliveredAt() == null
                || LocalDateTime.now(clock).isAfter(order.getDeliveredAt().plusDays(orderProperties.returnDays()))) {
            return "Đã quá hạn yêu cầu trả hàng (" + orderProperties.returnDays() + " ngày kể từ khi nhận hàng).";
        }
        return null;
    }

    private static boolean isAwaitingPayment(Order o) {
        return o.getPaymentMethod() != PaymentMethod.COD && o.getPayment() != null
                && o.getPayment().getStatus() == PaymentStatus.PENDING;
    }

    private void deleteOnRollback(List<String> publicIds) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        publicIds.forEach(UserOrderServiceImpl.this::deleteQuietly);
                    }
                }
            });
        }
    }

    private void deleteQuietly(String publicId) {
        if (!StringUtils.hasText(publicId)) {
            return;
        }
        try {
            fileStorageService.delete(publicId, MediaType.IMAGE);
        } catch (FileStorageException e) {
            log.warn("Không xóa được ảnh trả hàng {}: {}", publicId, e.getMessage());
        }
    }
}
