package com.starshop.service.impl;

import com.starshop.entity.Order;
import com.starshop.entity.OrderItem;
import com.starshop.entity.OrderStatusHistory;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.OrderStatusHistoryRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.OrderService;
import com.starshop.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /**
     * Luồng trạng thái hợp lệ (hợp đồng chung trong OrderStatus), thêm một nhánh đã thống nhất:
     * RETURN_REQUESTED → DELIVERED khi vendor từ chối yêu cầu trả hàng.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(OrderStatus.NEW, EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PICKED_UP, OrderStatus.CANCELLED));
        TRANSITIONS.put(OrderStatus.PICKED_UP, EnumSet.of(OrderStatus.SHIPPING));
        TRANSITIONS.put(OrderStatus.SHIPPING, EnumSet.of(OrderStatus.DELIVERED));
        TRANSITIONS.put(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.RETURN_REQUESTED));
        TRANSITIONS.put(OrderStatus.RETURN_REQUESTED, EnumSet.of(OrderStatus.REFUNDED, OrderStatus.DELIVERED));
        TRANSITIONS.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        TRANSITIONS.put(OrderStatus.REFUNDED, EnumSet.noneOf(OrderStatus.class));
    }

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PromotionService promotionService;
    private final Clock clock;

    @Override
    public boolean canTransition(OrderStatus from, OrderStatus to) {
        return from != null && to != null && TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
    }

    @Override
    @Transactional
    public void changeStatus(Long orderId, OrderStatus to, Long changedByUserId, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
        OrderStatus from = order.getStatus();
        if (!canTransition(from, to)) {
            throw new BusinessException("Không thể chuyển đơn " + order.getCode() + " từ \"" + from.getLabel()
                    + "\" sang \"" + to.getLabel() + "\".");
        }
        order.setStatus(to);
        if (to == OrderStatus.CANCELLED) {
            order.setCancelReason(note);
        } else if (to == OrderStatus.DELIVERED && order.getDeliveredAt() == null) {
            // Chỉ ghi ngày giao lần đầu: từ chối trả hàng (RETURN_REQUESTED -> DELIVERED) giữ nguyên ngày cũ
            order.setDeliveredAt(LocalDateTime.now(clock));
        }
        historyRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedByUserId == null ? null : userRepository.getReferenceById(changedByUserId))
                .note(note)
                .build());

        if (to == OrderStatus.CANCELLED) {
            for (OrderItem item : order.getItems()) {
                productRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
            }
            // Để cuối: câu UPDATE trả lượt mã sẽ flush rồi clear persistence context
            promotionService.releaseUsage(orderId);
        }
    }

    @Override
    @Transactional
    public void addHistoryNote(Long orderId, Long changedByUserId, String note) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn hàng"));
        historyRepository.save(OrderStatusHistory.builder()
                .order(order)
                .fromStatus(order.getStatus())
                .toStatus(order.getStatus())
                .changedBy(changedByUserId == null ? null : userRepository.getReferenceById(changedByUserId))
                .note(note)
                .build());
    }
}
