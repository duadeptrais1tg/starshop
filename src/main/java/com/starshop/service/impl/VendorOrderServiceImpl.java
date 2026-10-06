package com.starshop.service.impl;

import com.starshop.dto.order.VendorOrderDetail;
import com.starshop.dto.order.VendorOrderRow;
import com.starshop.dto.order.VendorOrderTab;
import com.starshop.entity.Order;
import com.starshop.entity.Payment;
import com.starshop.entity.ReturnRequest;
import com.starshop.entity.ShipperAssignment;
import com.starshop.entity.Shop;
import com.starshop.entity.User;
import com.starshop.entity.enums.AssignmentStatus;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.entity.enums.PaymentMethod;
import com.starshop.entity.enums.PaymentStatus;
import com.starshop.entity.enums.ReturnStatus;
import com.starshop.entity.enums.RoleName;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.OrderItemRepository;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.OrderStatusHistoryRepository;
import com.starshop.repository.ReturnRequestRepository;
import com.starshop.repository.ShipperAssignmentRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.UserRepository;
import com.starshop.repository.spec.OrderSpecifications;
import com.starshop.service.OrderService;
import com.starshop.service.VendorOrderService;
import com.starshop.util.DateFormats;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VendorOrderServiceImpl implements VendorOrderService {

    private static final int MAX_REASON_LENGTH = 500;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final ShopRepository shopRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ShipperAssignmentRepository assignmentRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @Override
    @Transactional(readOnly = true)
    public Page<VendorOrderRow> search(Long ownerId, VendorOrderTab tab, String code, LocalDate from, LocalDate to, int page) {
        Shop shop = requireShop(ownerId);
        VendorOrderTab selected = tab == null ? VendorOrderTab.NEW : tab;
        Specification<Order> spec = Specification.allOf(filter(shop.getId(), code, from, to),
                OrderSpecifications.statusIn(selected.getStatuses()));
        // Tab chờ xử lý: đơn cũ nhất lên đầu; các tab khác: mới nhất lên đầu
        Sort sort = selected == VendorOrderTab.NEW || selected == VendorOrderTab.CONFIRMED
                ? Sort.by("id") : Sort.by(Sort.Direction.DESC, "id");
        Page<Order> orders = orderRepository.findAll(spec, PageRequest.of(Math.max(page, 0), PAGE_SIZE, sort));
        Map<Long, Long> quantities = new HashMap<>();
        if (orders.hasContent()) {
            for (Object[] row : orderItemRepository.sumQuantityByOrderIds(orders.map(Order::getId).getContent())) {
                quantities.put((Long) row[0], (Long) row[1]);
            }
        }
        return orders.map(o -> VendorOrderRow.builder()
                        .id(o.getId())
                        .code(o.getCode())
                        .createdAt(DateFormats.dateTime(o.getCreatedAt()))
                        .receiverName(o.getReceiverName())
                        .receiverPhone(o.getReceiverPhone())
                        .itemCount(quantities.getOrDefault(o.getId(), 0L).intValue())
                        .total(o.getTotal())
                        .paymentMethodLabel(o.getPaymentMethod().getLabel())
                        .paid(isPaidOnline(o))
                        .status(o.getStatus())
                        .carrierName(o.getCarrier().getName())
                        .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<VendorOrderTab, Long> countByTab(Long ownerId, String code, LocalDate from, LocalDate to) {
        Shop shop = requireShop(ownerId);
        Map<VendorOrderTab, Long> counts = new EnumMap<>(VendorOrderTab.class);
        for (VendorOrderTab tab : VendorOrderTab.values()) {
            counts.put(tab, orderRepository.count(Specification.allOf(filter(shop.getId(), code, from, to),
                    OrderSpecifications.statusIn(tab.getStatuses()))));
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public VendorOrderDetail detail(Long ownerId, Long orderId) {
        Order o = requireOwnOrder(ownerId, orderId);
        Payment payment = o.getPayment();
        BigDecimal merchandise = o.getSubtotal().subtract(o.getProductDiscount());
        BigDecimal shopReceives = merchandise.multiply(HUNDRED.subtract(o.getCommissionRate()))
                .divide(HUNDRED, 0, RoundingMode.HALF_UP);

        ShipperAssignment latest = assignmentRepository.findFirstByOrderIdOrderByIdDesc(o.getId()).orElse(null);
        boolean reassignable = isReassignable(o, latest);
        VendorOrderDetail.Assignment assignment = Optional.ofNullable(latest)
                .map(a -> VendorOrderDetail.Assignment.builder()
                        .shipperName(a.getShipper().getFullName())
                        .shipperPhone(a.getShipper().getPhone())
                        .statusLabel(a.getStatus().getLabel())
                        .failReason(a.getFailReason())
                        .assignedAt(DateFormats.dateTime(a.getCreatedAt()))
                        .build())
                .orElse(null);
        VendorOrderDetail.ReturnInfo returnInfo = returnRequestRepository.findByOrderId(o.getId())
                .map(r -> VendorOrderDetail.ReturnInfo.builder()
                        .reason(r.getReason())
                        .statusLabel(r.getStatus().getLabel())
                        .pending(r.getStatus() == ReturnStatus.PENDING)
                        .rejectReason(r.getRejectReason())
                        .imageUrls(List.copyOf(r.getImageUrls()))
                        .createdAt(DateFormats.dateTime(r.getCreatedAt()))
                        .build())
                .orElse(null);

        return VendorOrderDetail.builder()
                .id(o.getId())
                .code(o.getCode())
                .createdAt(DateFormats.dateTime(o.getCreatedAt()))
                .status(o.getStatus())
                .customerName(o.getUser().getFullName())
                .receiverName(o.getReceiverName())
                .receiverPhone(o.getReceiverPhone())
                .shippingAddress(o.getShippingAddress())
                .note(o.getNote())
                .cancelReason(o.getCancelReason())
                .carrierName(o.getCarrier().getName())
                .paymentMethodLabel(o.getPaymentMethod().getLabel())
                .paymentStatusLabel(payment == null ? null : payment.getStatus().getLabel())
                .paid(isPaidOnline(o))
                .couponCode(o.getCoupon() == null ? null : o.getCoupon().getCode())
                .subtotal(o.getSubtotal())
                .productDiscount(o.getProductDiscount())
                .shippingFee(o.getShippingFee())
                .shippingDiscount(o.getShippingDiscount())
                .total(o.getTotal())
                .commissionRate(o.getCommissionRate())
                .shopReceives(shopReceives)
                .items(o.getItems().stream().map(i -> VendorOrderDetail.Line.builder()
                        .productName(i.getProductName())
                        .productSlug(i.getProduct().getSlug())
                        .imageUrl(i.getProductImage())
                        .unitPrice(i.getUnitPrice())
                        .quantity(i.getQuantity())
                        .lineTotal(i.getLineTotal())
                        .build()).toList())
                .history(historyRepository.findByOrderIdOrderByCreatedAtAsc(o.getId()).stream()
                        .map(h -> VendorOrderDetail.History.builder()
                                .time(DateFormats.dateTime(h.getCreatedAt()))
                                .fromLabel(h.getFromStatus() == null ? null : h.getFromStatus().getLabel())
                                .toLabel(h.getToStatus().getLabel())
                                .changedBy(h.getChangedBy() == null ? "Hệ thống" : h.getChangedBy().getFullName())
                                .note(h.getNote())
                                .build()).toList())
                .assignment(assignment)
                .returnRequest(returnInfo)
                .shippers(o.getStatus() == OrderStatus.CONFIRMED || reassignable ? shipperOptions(o.getCarrier().getId()) : List.of())
                .reassignable(reassignable)
                .build();
    }

    @Override
    @Transactional
    public void confirm(Long ownerId, Long orderId) {
        Order order = requireOwnOrder(ownerId, orderId);
        orderService.changeStatus(order.getId(), OrderStatus.CONFIRMED, ownerId, "Shop xác nhận đơn");
    }

    @Override
    @Transactional
    public void cancel(Long ownerId, Long orderId, String reason) {
        Order order = requireOwnOrder(ownerId, orderId);
        String note = "Shop hủy: " + requireReason(reason);
        if (isPaidOnline(order)) {
            // Đơn đã thanh toán online: ghi chú để đối soát hoàn tiền cho khách
            note += " (khách đã thanh toán " + order.getPaymentMethod().getLabel() + ", cần hoàn tiền)";
        }
        orderService.changeStatus(order.getId(), OrderStatus.CANCELLED, ownerId, truncate(note));
    }

    @Override
    @Transactional
    public void assignShipper(Long ownerId, Long orderId, Long shipperId) {
        Order order = requireOwnOrder(ownerId, orderId);
        boolean reassign = isReassignable(order, assignmentRepository.findFirstByOrderIdOrderByIdDesc(order.getId()).orElse(null));
        if (order.getStatus() != OrderStatus.CONFIRMED && !reassign) {
            throw new BusinessException("Chỉ giao cho shipper khi đơn đã được xác nhận (đơn đang \""
                    + order.getStatus().getLabel() + "\").");
        }
        User shipper = shipperId == null ? null : userRepository.findById(shipperId).orElse(null);
        if (shipper == null || !shipper.hasRole(RoleName.SHIPPER) || !shipper.isEnabled() || shipper.isLocked()
                || shipper.getCarrier() == null || !Objects.equals(shipper.getCarrier().getId(), order.getCarrier().getId())) {
            throw new BusinessException("Vui lòng chọn shipper đang hoạt động của " + order.getCarrier().getName() + ".");
        }
        assignmentRepository.save(ShipperAssignment.builder()
                .order(order)
                .shipper(shipper)
                .assignedBy(userRepository.getReferenceById(ownerId))
                .build());
        if (reassign) {
            // Lần trước giao thất bại: đơn vẫn Đang giao, chỉ đổi người giao
            orderService.addHistoryNote(order.getId(), ownerId, "Giao lại cho shipper " + shipper.getFullName());
        } else {
            orderService.changeStatus(order.getId(), OrderStatus.PICKED_UP, ownerId,
                    "Giao cho shipper " + shipper.getFullName());
        }
    }

    @Override
    @Transactional
    public void approveReturn(Long ownerId, Long orderId) {
        Order order = requireOwnOrder(ownerId, orderId);
        ReturnRequest request = requirePendingReturn(order);
        request.setStatus(ReturnStatus.APPROVED);
        orderService.changeStatus(order.getId(), OrderStatus.REFUNDED, ownerId, "Shop chấp nhận trả hàng, hoàn tiền");
    }

    @Override
    @Transactional
    public void rejectReturn(Long ownerId, Long orderId, String reason) {
        Order order = requireOwnOrder(ownerId, orderId);
        ReturnRequest request = requirePendingReturn(order);
        String validReason = requireReason(reason);
        request.setStatus(ReturnStatus.REJECTED);
        request.setRejectReason(validReason);
        orderService.changeStatus(order.getId(), OrderStatus.DELIVERED, ownerId, truncate("Shop từ chối trả hàng: " + validReason));
    }

    // ------------------------------------------------------------------ helpers

    private Shop requireShop(Long ownerId) {
        return shopRepository.findByOwnerId(ownerId)
                .filter(s -> s.getStatus() == ShopStatus.APPROVED || s.getStatus() == ShopStatus.SUSPENDED)
                .orElseThrow(() -> new NotFoundException("Bạn chưa có shop đang hoạt động"));
    }

    /** Đơn phải thuộc shop của vendor và shop được thấy (COD / đã thanh toán); nếu không coi như không tồn tại. */
    private Order requireOwnOrder(Long ownerId, Long orderId) {
        Shop shop = requireShop(ownerId);
        return orderRepository.findById(orderId)
                .filter(o -> o.getShop().getId().equals(shop.getId()))
                .filter(o -> o.getPaymentMethod() == PaymentMethod.COD || isPaidOnline(o))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
    }

    private static Specification<Order> filter(Long shopId, String code, LocalDate from, LocalDate to) {
        return Specification.allOf(
                OrderSpecifications.shop(shopId),
                OrderSpecifications.visibleToShop(),
                OrderSpecifications.codeContains(code),
                OrderSpecifications.createdFrom(from),
                OrderSpecifications.createdTo(to));
    }

    private ReturnRequest requirePendingReturn(Order order) {
        if (order.getStatus() != OrderStatus.RETURN_REQUESTED) {
            throw new BusinessException("Đơn " + order.getCode() + " không có yêu cầu trả hàng đang chờ duyệt.");
        }
        return returnRequestRepository.findByOrderId(order.getId())
                .filter(r -> r.getStatus() == ReturnStatus.PENDING)
                .orElseThrow(() -> new BusinessException("Yêu cầu trả hàng đã được xử lý."));
    }

    private List<VendorOrderDetail.ShipperOption> shipperOptions(Long carrierId) {
        List<User> shippers = userRepository.findActiveShippersByCarrierId(carrierId);
        Map<Long, Long> active = new HashMap<>();
        if (!shippers.isEmpty()) {
            for (Object[] row : assignmentRepository.countActiveByShipperIds(shippers.stream().map(User::getId).toList())) {
                active.put((Long) row[0], (Long) row[1]);
            }
        }
        return shippers.stream().map(u -> VendorOrderDetail.ShipperOption.builder()
                .id(u.getId())
                .name(u.getFullName())
                .phone(u.getPhone())
                .activeOrders(active.getOrDefault(u.getId(), 0L))
                .build()).toList();
    }

    /** Đơn đang giao mà lần giao gần nhất thất bại -> vendor giao lại cho shipper khác. */
    private static boolean isReassignable(Order order, ShipperAssignment latest) {
        return order.getStatus() == OrderStatus.SHIPPING && latest != null
                && latest.getStatus() == AssignmentStatus.FAILED;
    }

    private static boolean isPaidOnline(Order o) {
        return o.getPaymentMethod() != PaymentMethod.COD && o.getPayment() != null
                && (o.getPayment().getStatus() == PaymentStatus.PAID || o.getPayment().getStatus() == PaymentStatus.REFUNDED);
    }

    private static String requireReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("Vui lòng nhập lý do.");
        }
        String trimmed = reason.trim();
        if (trimmed.length() > 300) {
            throw new BusinessException("Lý do tối đa 300 ký tự.");
        }
        return trimmed;
    }

    private static String truncate(String note) {
        return note.length() > MAX_REASON_LENGTH ? note.substring(0, MAX_REASON_LENGTH) : note;
    }
}
