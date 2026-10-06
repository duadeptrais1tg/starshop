package com.starshop.service.impl;

import com.starshop.entity.Carrier;
import com.starshop.entity.Order;
import com.starshop.entity.Payment;
import com.starshop.entity.ReturnRequest;
import com.starshop.entity.Role;
import com.starshop.entity.ShipperAssignment;
import com.starshop.entity.Shop;
import com.starshop.entity.User;
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
import com.starshop.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VendorOrderServiceImplTest {

    private static final Long OWNER_ID = 3L;

    private final ShopRepository shopRepository = mock(ShopRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ShipperAssignmentRepository assignmentRepository = mock(ShipperAssignmentRepository.class);
    private final ReturnRequestRepository returnRepository = mock(ReturnRequestRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final OrderService orderService = mock(OrderService.class);
    private final VendorOrderServiceImpl service = new VendorOrderServiceImpl(shopRepository, orderRepository,
            mock(OrderItemRepository.class), mock(OrderStatusHistoryRepository.class), assignmentRepository,
            returnRepository, userRepository, orderService);

    private Shop myShop;
    private Carrier ghn;
    private Order order;

    @BeforeEach
    void setUp() {
        myShop = Shop.builder().id(1L).status(ShopStatus.APPROVED).build();
        when(shopRepository.findByOwnerId(OWNER_ID)).thenReturn(Optional.of(myShop));
        ghn = Carrier.builder().id(10L).name("Giao Hàng Nhanh").build();
        order = Order.builder().id(100L).code("SS1").shop(myShop).carrier(ghn)
                .paymentMethod(PaymentMethod.COD).status(OrderStatus.NEW).build();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
    }

    @Test
    void otherShopsOrder_orUnpaidOnlineOrder_isNotFound() {
        order.setShop(Shop.builder().id(2L).build());
        assertThatThrownBy(() -> service.confirm(OWNER_ID, 100L)).isInstanceOf(NotFoundException.class);

        order.setShop(myShop);
        order.setPaymentMethod(PaymentMethod.VNPAY);
        order.setPayment(Payment.builder().status(PaymentStatus.PENDING).build());
        assertThatThrownBy(() -> service.confirm(OWNER_ID, 100L)).isInstanceOf(NotFoundException.class);
        verify(orderService, never()).changeStatus(anyLong(), any(), any(), any());
    }

    @Test
    void confirm_goesThroughOrderService() {
        service.confirm(OWNER_ID, 100L);
        verify(orderService).changeStatus(100L, OrderStatus.CONFIRMED, OWNER_ID, "Shop xác nhận đơn");
    }

    @Test
    void wrongTransition_errorFromOrderService_propagates() {
        order.setStatus(OrderStatus.SHIPPING);
        when(orderService.canTransition(any(), any())).thenReturn(false);
        org.mockito.Mockito.doThrow(new BusinessException("Không thể chuyển đơn SS1"))
                .when(orderService).changeStatus(eq(100L), eq(OrderStatus.CANCELLED), any(), any());

        assertThatThrownBy(() -> service.cancel(OWNER_ID, 100L, "Hết hoa")).hasMessageContaining("Không thể chuyển");
    }

    @Test
    void cancel_requiresReason_andNotesRefundForPaidOnlineOrders() {
        assertThatThrownBy(() -> service.cancel(OWNER_ID, 100L, "  ")).hasMessageContaining("lý do");

        order.setPaymentMethod(PaymentMethod.VNPAY);
        order.setPayment(Payment.builder().status(PaymentStatus.PAID).build());
        service.cancel(OWNER_ID, 100L, "Hết hoa hồng");

        verify(orderService).changeStatus(eq(100L), eq(OrderStatus.CANCELLED), eq(OWNER_ID),
                contains("cần hoàn tiền"));
    }

    @Test
    void assignShipper_validatesStatusAndShipper() {
        User shipper = shipper(50L, ghn);
        assertThatThrownBy(() -> service.assignShipper(OWNER_ID, 100L, 50L))
                .hasMessageContaining("đã được xác nhận");   // đơn đang NEW

        order.setStatus(OrderStatus.CONFIRMED);
        shipper.setCarrier(Carrier.builder().id(11L).build());
        assertThatThrownBy(() -> service.assignShipper(OWNER_ID, 100L, 50L)).hasMessageContaining("Giao Hàng Nhanh");

        shipper.setCarrier(ghn);
        shipper.setLocked(true);
        assertThatThrownBy(() -> service.assignShipper(OWNER_ID, 100L, 50L)).isInstanceOf(BusinessException.class);

        User notShipper = shipper(51L, ghn);
        notShipper.setRoles(new HashSet<>(Set.of(Role.builder().name(RoleName.USER).build())));
        assertThatThrownBy(() -> service.assignShipper(OWNER_ID, 100L, 51L)).isInstanceOf(BusinessException.class);

        verify(assignmentRepository, never()).save(any());
        verify(orderService, never()).changeStatus(anyLong(), any(), any(), any());
    }

    @Test
    void assignShipper_createsAssignment_thenPickedUp() {
        order.setStatus(OrderStatus.CONFIRMED);
        User shipper = shipper(50L, ghn);

        service.assignShipper(OWNER_ID, 100L, 50L);

        ArgumentCaptor<ShipperAssignment> captor = ArgumentCaptor.forClass(ShipperAssignment.class);
        verify(assignmentRepository).save(captor.capture());
        assertThat(captor.getValue().getShipper()).isSameAs(shipper);
        assertThat(captor.getValue().getOrder()).isSameAs(order);
        verify(orderService).changeStatus(eq(100L), eq(OrderStatus.PICKED_UP), eq(OWNER_ID), contains("Shipper Một"));
    }

    @Test
    void returns_approveAndReject() {
        order.setStatus(OrderStatus.RETURN_REQUESTED);
        ReturnRequest request = ReturnRequest.builder().order(order).reason("Hoa héo").build();
        when(returnRepository.findByOrderId(100L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> service.rejectReturn(OWNER_ID, 100L, "")).hasMessageContaining("lý do");
        assertThat(request.getStatus()).isEqualTo(ReturnStatus.PENDING);

        service.rejectReturn(OWNER_ID, 100L, "Ảnh không cho thấy hoa héo");
        assertThat(request.getStatus()).isEqualTo(ReturnStatus.REJECTED);
        assertThat(request.getRejectReason()).isEqualTo("Ảnh không cho thấy hoa héo");
        verify(orderService).changeStatus(eq(100L), eq(OrderStatus.DELIVERED), eq(OWNER_ID), contains("từ chối"));

        // Đã xử lý rồi thì không duyệt lại được
        assertThatThrownBy(() -> service.approveReturn(OWNER_ID, 100L)).hasMessageContaining("đã được xử lý");

        request.setStatus(ReturnStatus.PENDING);
        service.approveReturn(OWNER_ID, 100L);
        assertThat(request.getStatus()).isEqualTo(ReturnStatus.APPROVED);
        verify(orderService).changeStatus(eq(100L), eq(OrderStatus.REFUNDED), eq(OWNER_ID), any());
    }

    @Test
    void returnActions_requireReturnRequestedStatus() {
        order.setStatus(OrderStatus.DELIVERED);
        assertThatThrownBy(() -> service.approveReturn(OWNER_ID, 100L)).hasMessageContaining("không có yêu cầu trả hàng");
    }

    private User shipper(Long id, Carrier carrier) {
        User u = User.builder().id(id).fullName("Shipper Một").phone("0900000000").enabled(true).carrier(carrier)
                .roles(new HashSet<>(Set.of(Role.builder().name(RoleName.SHIPPER).build()))).build();
        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        return u;
    }
}
