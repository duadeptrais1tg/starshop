package com.starshop.service.impl;

import com.starshop.dto.order.ShipperOrderDetail;
import com.starshop.dto.order.ShipperOrderRow;
import com.starshop.dto.order.ShipperStats;
import com.starshop.entity.Order;
import com.starshop.entity.ShipperAssignment;
import com.starshop.entity.enums.AssignmentStatus;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.entity.enums.PaymentMethod;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.ShipperAssignmentRepository;
import com.starshop.service.OrderService;
import com.starshop.service.ShipperOrderService;
import com.starshop.util.DateFormats;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ShipperOrderServiceImpl implements ShipperOrderService {

    private static final int MAX_REASON_LENGTH = 300;

    private final ShipperAssignmentRepository assignmentRepository;
    private final OrderService orderService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Page<ShipperOrderRow> search(Long shipperId, AssignmentStatus status, int page) {
        AssignmentStatus selected = status == null ? AssignmentStatus.ASSIGNED : status;
        // Việc cần làm: cũ nhất lên đầu; lịch sử: mới nhất lên đầu
        Sort sort = selected == AssignmentStatus.ASSIGNED || selected == AssignmentStatus.DELIVERING
                ? Sort.by("id") : Sort.by(Sort.Direction.DESC, "id");
        return assignmentRepository.findByShipperIdAndStatus(shipperId, selected, PageRequest.of(Math.max(page, 0), PAGE_SIZE, sort))
                .map(a -> {
                    Order o = a.getOrder();
                    return ShipperOrderRow.builder()
                            .id(a.getId())
                            .orderCode(o.getCode())
                            .assignedAt(DateFormats.dateTime(a.getCreatedAt()))
                            .shopName(o.getShop().getName())
                            .receiverName(o.getReceiverName())
                            .receiverPhone(o.getReceiverPhone())
                            .shippingAddress(o.getShippingAddress())
                            .codAmount(codAmount(o))
                            .status(a.getStatus())
                            .build();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Map<AssignmentStatus, Long> countByStatus(Long shipperId) {
        Map<AssignmentStatus, Long> counts = new EnumMap<>(AssignmentStatus.class);
        for (AssignmentStatus s : AssignmentStatus.values()) {
            counts.put(s, 0L);
        }
        for (Object[] row : assignmentRepository.countByStatus(shipperId)) {
            counts.put((AssignmentStatus) row[0], (Long) row[1]);
        }
        return counts;
    }

    @Override
    @Transactional(readOnly = true)
    public ShipperOrderDetail detail(Long shipperId, Long assignmentId) {
        ShipperAssignment a = requireOwn(shipperId, assignmentId);
        Order o = a.getOrder();
        return ShipperOrderDetail.builder()
                .id(a.getId())
                .orderCode(o.getCode())
                .orderStatus(o.getStatus())
                .status(a.getStatus())
                .assignedAt(DateFormats.dateTime(a.getCreatedAt()))
                .deliveredAt(a.getDeliveredAt() == null ? null : DateFormats.dateTime(a.getDeliveredAt()))
                .failReason(a.getFailReason())
                .shopName(o.getShop().getName())
                .shopPhone(o.getShop().getPhone())
                .pickupAddress(o.getShop().getPickupAddress())
                .receiverName(o.getReceiverName())
                .receiverPhone(o.getReceiverPhone())
                .shippingAddress(o.getShippingAddress())
                .note(o.getNote())
                .paymentMethodLabel(o.getPaymentMethod().getLabel())
                .cod(o.getPaymentMethod() == PaymentMethod.COD)
                .codAmount(codAmount(o))
                .codCollected(a.isCodCollected())
                .collectedAt(a.getCollectedAt() == null ? null : DateFormats.dateTime(a.getCollectedAt()))
                .items(o.getItems().stream().map(i -> ShipperOrderDetail.Line.builder()
                        .productName(i.getProductName()).quantity(i.getQuantity()).build()).toList())
                .current(isCurrent(a))
                .build();
    }

    @Override
    @Transactional
    public void startDelivery(Long shipperId, Long assignmentId) {
        ShipperAssignment a = requireCurrent(shipperId, assignmentId);
        requireStatus(a, AssignmentStatus.ASSIGNED, "bắt đầu giao");
        Long orderId = a.getOrder().getId();
        a.setStatus(AssignmentStatus.DELIVERING);
        if (a.getOrder().getStatus() == OrderStatus.SHIPPING) {
            // Đơn được giao lại cho shipper khác sau lần thất bại: đã ở trạng thái Đang giao
            orderService.addHistoryNote(orderId, shipperId, a.getShipper().getFullName() + " (shipper) bắt đầu giao lại");
        } else {
            orderService.changeStatus(orderId, OrderStatus.SHIPPING, shipperId,
                    a.getShipper().getFullName() + " (shipper) bắt đầu giao");
        }
    }

    @Override
    @Transactional
    public void markDelivered(Long shipperId, Long assignmentId) {
        ShipperAssignment a = requireCurrent(shipperId, assignmentId);
        requireStatus(a, AssignmentStatus.DELIVERING, "xác nhận đã giao");
        LocalDateTime now = LocalDateTime.now(clock);
        Order order = a.getOrder();
        boolean cod = order.getPaymentMethod() == PaymentMethod.COD;
        String note = cod ? "Giao thành công, đã thu " + money(order.getTotal()) + "₫" : "Giao thành công";

        a.setStatus(AssignmentStatus.DELIVERED);
        a.setDeliveredAt(now);
        a.setFailReason(null);
        if (cod) {
            a.setCodCollected(true);
            a.setCollectedAt(now);
        }
        orderService.changeStatus(order.getId(), OrderStatus.DELIVERED, shipperId, note);
    }

    @Override
    @Transactional
    public void markFailed(Long shipperId, Long assignmentId, String reason) {
        ShipperAssignment a = requireCurrent(shipperId, assignmentId);
        requireStatus(a, AssignmentStatus.DELIVERING, "báo giao thất bại");
        if (!StringUtils.hasText(reason)) {
            throw new BusinessException("Vui lòng nhập lý do giao thất bại.");
        }
        String text = reason.trim();
        if (text.length() > MAX_REASON_LENGTH) {
            throw new BusinessException("Lý do tối đa " + MAX_REASON_LENGTH + " ký tự.");
        }
        a.setStatus(AssignmentStatus.FAILED);
        a.setFailReason(text);
        orderService.addHistoryNote(a.getOrder().getId(), shipperId, "Giao thất bại: " + text);
    }

    @Override
    @Transactional
    public void retry(Long shipperId, Long assignmentId) {
        ShipperAssignment a = requireCurrent(shipperId, assignmentId);
        requireStatus(a, AssignmentStatus.FAILED, "giao lại");
        if (a.getOrder().getStatus() != OrderStatus.SHIPPING) {
            throw new BusinessException("Đơn " + a.getOrder().getCode() + " không còn ở trạng thái Đang giao.");
        }
        a.setStatus(AssignmentStatus.DELIVERING);
        orderService.addHistoryNote(a.getOrder().getId(), shipperId, "Shipper giao lại");
    }

    @Override
    @Transactional(readOnly = true)
    public ShipperStats stats(Long shipperId) {
        Map<AssignmentStatus, Long> counts = countByStatus(shipperId);
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        YearMonth current = YearMonth.now(clock);
        YearMonth first = current.minusMonths(STATS_MONTHS - 1L);
        Map<YearMonth, Map<AssignmentStatus, Long>> byMonth = new HashMap<>();
        for (Object[] row : assignmentRepository.monthlyStats(shipperId, first.atDay(1).atStartOfDay())) {
            YearMonth month = YearMonth.of(((Number) row[0]).intValue(), ((Number) row[1]).intValue());
            byMonth.computeIfAbsent(month, k -> new EnumMap<>(AssignmentStatus.class))
                    .put((AssignmentStatus) row[2], ((Number) row[3]).longValue());
        }
        List<ShipperStats.Month> months = new ArrayList<>();
        for (YearMonth m = current; !m.isBefore(first); m = m.minusMonths(1)) {
            Map<AssignmentStatus, Long> c = byMonth.getOrDefault(m, Map.of());
            months.add(ShipperStats.Month.builder()
                    .label(String.format("%02d/%d", m.getMonthValue(), m.getYear()))
                    .assigned(c.values().stream().mapToLong(Long::longValue).sum())
                    .delivered(c.getOrDefault(AssignmentStatus.DELIVERED, 0L))
                    .failed(c.getOrDefault(AssignmentStatus.FAILED, 0L))
                    .build());
        }
        return ShipperStats.builder()
                .assigned(total)
                .waiting(counts.get(AssignmentStatus.ASSIGNED))
                .delivering(counts.get(AssignmentStatus.DELIVERING))
                .delivered(counts.get(AssignmentStatus.DELIVERED))
                .failed(counts.get(AssignmentStatus.FAILED))
                .codCollectedThisMonth(assignmentRepository.sumCodCollectedSince(shipperId, current.atDay(1).atStartOfDay()))
                .months(months)
                .build();
    }

    // ------------------------------------------------------------------ helpers

    /** Lần phân công phải của chính shipper; của người khác coi như không tồn tại. */
    private ShipperAssignment requireOwn(Long shipperId, Long assignmentId) {
        return assignmentRepository.findByIdAndShipperId(assignmentId, shipperId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn được phân công"));
    }

    /** Chỉ thao tác trên lần phân công mới nhất của đơn (đơn đã giao cho shipper khác thì chỉ xem). */
    private ShipperAssignment requireCurrent(Long shipperId, Long assignmentId) {
        ShipperAssignment a = requireOwn(shipperId, assignmentId);
        if (!isCurrent(a)) {
            throw new BusinessException("Đơn " + a.getOrder().getCode() + " đã được giao cho shipper khác.");
        }
        return a;
    }

    private boolean isCurrent(ShipperAssignment a) {
        return assignmentRepository.findFirstByOrderIdOrderByIdDesc(a.getOrder().getId())
                .map(latest -> latest.getId().equals(a.getId()))
                .orElse(false);
    }

    private static void requireStatus(ShipperAssignment a, AssignmentStatus expected, String action) {
        if (a.getStatus() != expected) {
            throw new BusinessException("Không thể " + action + " đơn " + a.getOrder().getCode() + " đang ở trạng thái \""
                    + a.getStatus().getLabel() + "\".");
        }
    }

    private static String money(BigDecimal value) {
        return String.format(Locale.forLanguageTag("vi-VN"), "%,.0f", value);
    }

    private static BigDecimal codAmount(Order o) {
        return o.getPaymentMethod() == PaymentMethod.COD ? o.getTotal() : BigDecimal.ZERO;
    }
}
