package com.starshop.service.impl;

import com.starshop.entity.Order;
import com.starshop.entity.OrderItem;
import com.starshop.entity.OrderStatusHistory;
import com.starshop.entity.Product;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.exception.BusinessException;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.OrderStatusHistoryRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Clock;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderStatusHistoryRepository historyRepository = mock(OrderStatusHistoryRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final PromotionService promotionService = mock(PromotionService.class);
    private final OrderServiceImpl service = new OrderServiceImpl(orderRepository, historyRepository, productRepository,
            mock(UserRepository.class), promotionService, Clock.systemDefaultZone());

    @Test
    void transitions_followTheContract() {
        assertThat(service.canTransition(OrderStatus.NEW, OrderStatus.CONFIRMED)).isTrue();
        assertThat(service.canTransition(OrderStatus.CONFIRMED, OrderStatus.CANCELLED)).isTrue();
        assertThat(service.canTransition(OrderStatus.DELIVERED, OrderStatus.RETURN_REQUESTED)).isTrue();
        assertThat(service.canTransition(OrderStatus.PICKED_UP, OrderStatus.CANCELLED)).isFalse();
        assertThat(service.canTransition(OrderStatus.NEW, OrderStatus.DELIVERED)).isFalse();
        assertThat(service.canTransition(OrderStatus.CANCELLED, OrderStatus.NEW)).isFalse();
        // Từ chối trả hàng
        assertThat(service.canTransition(OrderStatus.RETURN_REQUESTED, OrderStatus.DELIVERED)).isTrue();
        assertThat(service.canTransition(OrderStatus.REFUNDED, OrderStatus.DELIVERED)).isFalse();
    }

    @Test
    void cancel_restoresStock_releasesCoupon_last_andWritesHistory() {
        Order order = order(OrderStatus.NEW);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        service.changeStatus(1L, OrderStatus.CANCELLED, null, "Thanh toán thất bại");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelReason()).isEqualTo("Thanh toán thất bại");
        ArgumentCaptor<OrderStatusHistory> history = ArgumentCaptor.forClass(OrderStatusHistory.class);
        verify(historyRepository).save(history.capture());
        assertThat(history.getValue().getFromStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(history.getValue().getToStatus()).isEqualTo(OrderStatus.CANCELLED);
        InOrder sequence = inOrder(productRepository, promotionService);
        sequence.verify(productRepository).increaseStock(10L, 2);
        sequence.verify(productRepository).increaseStock(11L, 1);
        sequence.verify(promotionService).releaseUsage(1L);
    }

    @Test
    void invalidTransition_isRejected_withoutSideEffects() {
        Order order = order(OrderStatus.SHIPPING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.changeStatus(1L, OrderStatus.CANCELLED, 5L, "x"))
                .isInstanceOf(BusinessException.class).hasMessageContaining("Không thể chuyển");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);
        verify(productRepository, never()).increaseStock(anyLong(), anyInt());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void delivered_setsDeliveredAt_withoutTouchingStock() {
        Order order = order(OrderStatus.SHIPPING);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        service.changeStatus(1L, OrderStatus.DELIVERED, 5L, null);

        assertThat(order.getDeliveredAt()).isNotNull();
        verify(productRepository, never()).increaseStock(anyLong(), anyInt());
        verify(promotionService, never()).releaseUsage(anyLong());
    }

    @Test
    void rejectedReturn_keepsOriginalDeliveredAt() {
        Order order = order(OrderStatus.RETURN_REQUESTED);
        java.time.LocalDateTime delivered = java.time.LocalDateTime.of(2026, 10, 1, 9, 0);
        order.setDeliveredAt(delivered);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        service.changeStatus(1L, OrderStatus.DELIVERED, 5L, "Từ chối trả hàng");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getDeliveredAt()).isEqualTo(delivered);
    }

    private static Order order(OrderStatus status) {
        Order order = Order.builder().id(1L).code("SS1").status(status).build();
        order.addItem(OrderItem.builder().product(Product.builder().id(10L).build()).quantity(2).build());
        order.addItem(OrderItem.builder().product(Product.builder().id(11L).build()).quantity(1).build());
        return order;
    }
}
