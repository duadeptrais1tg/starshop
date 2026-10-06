package com.starshop.service.impl;

import com.starshop.dto.order.ShipperStats;
import com.starshop.entity.Order;
import com.starshop.entity.ShipperAssignment;
import com.starshop.entity.User;
import com.starshop.entity.enums.AssignmentStatus;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.entity.enums.PaymentMethod;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.ShipperAssignmentRepository;
import com.starshop.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShipperOrderServiceImplTest {

    private static final Long SHIPPER_ID = 4L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 15, 0);

    private final ShipperAssignmentRepository repository = mock(ShipperAssignmentRepository.class);
    private final OrderService orderService = mock(OrderService.class);
    private final ShipperOrderServiceImpl service = new ShipperOrderServiceImpl(repository, orderService,
            Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));

    private Order order;
    private ShipperAssignment assignment;

    @BeforeEach
    void setUp() {
        order = Order.builder().id(100L).code("SS1").status(OrderStatus.PICKED_UP)
                .paymentMethod(PaymentMethod.COD).total(new BigDecimal("580000")).build();
        assignment = ShipperAssignment.builder().id(9L).order(order)
                .shipper(User.builder().id(SHIPPER_ID).fullName("Shipper GHN").build()).build();
        when(repository.findByIdAndShipperId(9L, SHIPPER_ID)).thenReturn(Optional.of(assignment));
        when(repository.findFirstByOrderIdOrderByIdDesc(100L)).thenReturn(Optional.of(assignment));
    }

    @Test
    void otherShippersAssignment_isNotFound() {
        when(repository.findByIdAndShipperId(9L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.startDelivery(99L, 9L)).isInstanceOf(NotFoundException.class);
        verify(orderService, never()).changeStatus(anyLong(), any(), any(), any());
    }

    @Test
    void fullFlow_start_thenDelivered_collectsCod() {
        service.startDelivery(SHIPPER_ID, 9L);
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.DELIVERING);
        verify(orderService).changeStatus(eq(100L), eq(OrderStatus.SHIPPING), eq(SHIPPER_ID), anyString());

        service.markDelivered(SHIPPER_ID, 9L);
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.DELIVERED);
        assertThat(assignment.isCodCollected()).isTrue();
        assertThat(assignment.getCollectedAt()).isEqualTo(NOW);
        assertThat(assignment.getDeliveredAt()).isEqualTo(NOW);
        verify(orderService).changeStatus(eq(100L), eq(OrderStatus.DELIVERED), eq(SHIPPER_ID), contains("580.000"));
    }

    @Test
    void onlinePaidOrder_isNotMarkedAsCodCollected() {
        order.setPaymentMethod(PaymentMethod.VNPAY);
        assignment.setStatus(AssignmentStatus.DELIVERING);

        service.markDelivered(SHIPPER_ID, 9L);

        assertThat(assignment.isCodCollected()).isFalse();
        verify(orderService).changeStatus(100L, OrderStatus.DELIVERED, SHIPPER_ID, "Giao thành công");
    }

    @Test
    void wrongStep_isRejected() {
        assertThatThrownBy(() -> service.markDelivered(SHIPPER_ID, 9L)).hasMessageContaining("Đã phân công");
        assertThatThrownBy(() -> service.markFailed(SHIPPER_ID, 9L, "Không gọi được")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.retry(SHIPPER_ID, 9L)).isInstanceOf(BusinessException.class);
        verify(orderService, never()).changeStatus(anyLong(), any(), any(), any());
    }

    @Test
    void failed_keepsOrderShipping_thenRetry() {
        order.setStatus(OrderStatus.SHIPPING);
        assignment.setStatus(AssignmentStatus.DELIVERING);
        assertThatThrownBy(() -> service.markFailed(SHIPPER_ID, 9L, " ")).hasMessageContaining("lý do");

        service.markFailed(SHIPPER_ID, 9L, "  Không liên lạc được người nhận ");
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.FAILED);
        assertThat(assignment.getFailReason()).isEqualTo("Không liên lạc được người nhận");
        verify(orderService).addHistoryNote(100L, SHIPPER_ID, "Giao thất bại: Không liên lạc được người nhận");
        verify(orderService, never()).changeStatus(anyLong(), any(), any(), any());

        service.retry(SHIPPER_ID, 9L);
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.DELIVERING);
    }

    @Test
    void reassignedOrder_startDoesNotChangeOrderStatusAgain() {
        order.setStatus(OrderStatus.SHIPPING);   // vendor giao lại cho shipper này sau lần thất bại

        service.startDelivery(SHIPPER_ID, 9L);

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.DELIVERING);
        verify(orderService).addHistoryNote(eq(100L), eq(SHIPPER_ID), contains("giao lại"));
        verify(orderService, never()).changeStatus(anyLong(), any(), any(), any());
    }

    @Test
    void oldAssignment_afterReassign_isReadOnly() {
        assignment.setStatus(AssignmentStatus.FAILED);
        order.setStatus(OrderStatus.SHIPPING);
        when(repository.findFirstByOrderIdOrderByIdDesc(100L))
                .thenReturn(Optional.of(ShipperAssignment.builder().id(10L).build()));

        assertThatThrownBy(() -> service.retry(SHIPPER_ID, 9L)).hasMessageContaining("shipper khác");
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.FAILED);
    }

    @Test
    void stats_countsAndMonthlySuccessRate() {
        when(repository.countByStatus(SHIPPER_ID)).thenReturn(List.of(
                new Object[]{AssignmentStatus.DELIVERED, 9L}, new Object[]{AssignmentStatus.FAILED, 1L},
                new Object[]{AssignmentStatus.ASSIGNED, 2L}));
        when(repository.monthlyStats(eq(SHIPPER_ID), eq(LocalDateTime.of(2026, 5, 1, 0, 0)))).thenReturn(List.of(
                new Object[]{2026, 10, AssignmentStatus.DELIVERED, 3L},
                new Object[]{2026, 10, AssignmentStatus.FAILED, 1L},
                new Object[]{2026, 10, AssignmentStatus.ASSIGNED, 2L},
                new Object[]{2026, 9, AssignmentStatus.DELIVERED, 6L}));
        when(repository.sumCodCollectedSince(eq(SHIPPER_ID), any())).thenReturn(new BigDecimal("1200000"));

        ShipperStats stats = service.stats(SHIPPER_ID);

        assertThat(stats.getAssigned()).isEqualTo(12);
        assertThat(stats.getWaiting()).isEqualTo(2);
        assertThat(stats.getDelivering()).isZero();
        assertThat(stats.getSuccessRate()).isEqualTo(90);
        assertThat(stats.getMonths()).hasSize(6);
        ShipperStats.Month october = stats.getMonths().get(0);
        assertThat(october.getLabel()).isEqualTo("10/2026");
        assertThat(october.getAssigned()).isEqualTo(6);
        assertThat(october.getSuccessRate()).isEqualTo(75);
        assertThat(stats.getMonths().get(1).getSuccessRate()).isEqualTo(100);
        assertThat(stats.getMonths().get(5).getLabel()).isEqualTo("05/2026");
        assertThat(stats.getMonths().get(5).getSuccessRate()).isNull();
    }
}
