package com.starshop.service.impl;

import com.starshop.config.OrderProperties;
import com.starshop.dto.UploadResult;
import com.starshop.dto.order.CancelReason;
import com.starshop.entity.Order;
import com.starshop.entity.Payment;
import com.starshop.entity.ReturnRequest;
import com.starshop.entity.User;
import com.starshop.entity.enums.MediaType;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.entity.enums.PaymentMethod;
import com.starshop.entity.enums.PaymentStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.InvalidFileException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.OrderItemRepository;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.OrderStatusHistoryRepository;
import com.starshop.repository.ReturnRequestRepository;
import com.starshop.repository.ShipperAssignmentRepository;
import com.starshop.service.FileStorageService;
import com.starshop.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

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

class UserOrderServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 6, 12, 0);

    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final ReturnRequestRepository returnRepository = mock(ReturnRequestRepository.class);
    private final OrderService orderService = mock(OrderService.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final UserOrderServiceImpl service = new UserOrderServiceImpl(orderRepository, mock(OrderItemRepository.class),
            mock(OrderStatusHistoryRepository.class), mock(ShipperAssignmentRepository.class), returnRepository,
            orderService, storage, new OrderProperties(3), Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));

    private Order order;

    @BeforeEach
    void setUp() {
        order = Order.builder().id(100L).code("SS1").user(User.builder().id(USER_ID).build())
                .paymentMethod(PaymentMethod.COD).status(OrderStatus.NEW).build();
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(returnRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(storage.uploadImage(any(), eq("returns")))
                .thenReturn(new UploadResult("https://cdn/r.png", "returns/r", MediaType.IMAGE));
    }

    @Test
    void otherUsersOrder_isNotFound() {
        order.setUser(User.builder().id(8L).build());
        assertThatThrownBy(() -> service.cancel(USER_ID, 100L, CancelReason.NO_LONGER_NEEDED, null))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.requestReturn(USER_ID, 100L, "Hoa bị héo khi nhận", List.of(image())))
                .isInstanceOf(NotFoundException.class);
        verify(orderService, never()).changeStatus(anyLong(), any(), any(), any());
    }

    @Test
    void cancel_requiresReason_andGoesThroughOrderService() {
        assertThatThrownBy(() -> service.cancel(USER_ID, 100L, null, null)).hasMessageContaining("chọn lý do");
        assertThatThrownBy(() -> service.cancel(USER_ID, 100L, CancelReason.OTHER, " ")).hasMessageContaining("nhập lý do");

        service.cancel(USER_ID, 100L, CancelReason.OTHER, "  Đặt nhầm ngày giao  ");
        verify(orderService).changeStatus(100L, OrderStatus.CANCELLED, USER_ID, "Khách hủy: Đặt nhầm ngày giao");
    }

    @Test
    void cancel_onlyWhenNew_andNotAwaitingOnlinePayment() {
        order.setStatus(OrderStatus.CONFIRMED);
        assertThatThrownBy(() -> service.cancel(USER_ID, 100L, CancelReason.NO_LONGER_NEEDED, null))
                .hasMessageContaining("liên hệ shop");

        order.setStatus(OrderStatus.NEW);
        order.setPaymentMethod(PaymentMethod.VNPAY);
        order.setPayment(Payment.builder().status(PaymentStatus.PENDING).build());
        assertThatThrownBy(() -> service.cancel(USER_ID, 100L, CancelReason.NO_LONGER_NEEDED, null))
                .hasMessageContaining("chờ thanh toán");
        verify(orderService, never()).changeStatus(anyLong(), any(), any(), any());

        order.getPayment().setStatus(PaymentStatus.PAID);
        service.cancel(USER_ID, 100L, CancelReason.BETTER_PRICE, null);
        verify(orderService).changeStatus(eq(100L), eq(OrderStatus.CANCELLED), eq(USER_ID), contains("cần hoàn tiền"));
    }

    @Test
    void requestReturn_withinDeadline_savesRequest_thenReturnRequested() {
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(NOW.minusDays(2));

        service.requestReturn(USER_ID, 100L, "  Hoa bị dập, héo khi nhận  ", List.of(image(), image()));

        ArgumentCaptor<ReturnRequest> captor = ArgumentCaptor.forClass(ReturnRequest.class);
        verify(returnRepository).save(captor.capture());
        assertThat(captor.getValue().getReason()).isEqualTo("Hoa bị dập, héo khi nhận");
        assertThat(captor.getValue().getImageUrls()).hasSize(2);
        verify(orderService).changeStatus(100L, OrderStatus.RETURN_REQUESTED, USER_ID, "Khách yêu cầu trả hàng");
    }

    @Test
    void requestReturn_rules() {
        order.setStatus(OrderStatus.SHIPPING);
        assertThatThrownBy(() -> service.requestReturn(USER_ID, 100L, "Hoa bị héo khi nhận", List.of(image())))
                .hasMessageContaining("đã giao");

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(NOW.minusDays(3).minusMinutes(1));
        assertThatThrownBy(() -> service.requestReturn(USER_ID, 100L, "Hoa bị héo khi nhận", List.of(image())))
                .hasMessageContaining("quá hạn");

        order.setDeliveredAt(NOW.minusDays(1));
        assertThatThrownBy(() -> service.requestReturn(USER_ID, 100L, "ngắn", List.of(image())))
                .hasMessageContaining("Lý do");
        assertThatThrownBy(() -> service.requestReturn(USER_ID, 100L, "Hoa bị héo khi nhận", List.of()))
                .hasMessageContaining("ít nhất 1 ảnh");

        when(returnRepository.findByOrderId(100L)).thenReturn(Optional.of(ReturnRequest.builder().build()));
        assertThatThrownBy(() -> service.requestReturn(USER_ID, 100L, "Hoa bị héo khi nhận", List.of(image())))
                .hasMessageContaining("một lần");

        verify(storage, never()).uploadImage(any(), any());
        verify(orderService, never()).changeStatus(anyLong(), any(), any(), any());
    }

    @Test
    void requestReturn_uploadFails_deletesUploadedImages() {
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(NOW.minusDays(1));
        when(storage.uploadImage(any(), eq("returns")))
                .thenReturn(new UploadResult("https://cdn/a.png", "returns/a", MediaType.IMAGE))
                .thenThrow(new InvalidFileException("Ảnh tối đa 5MB"));

        assertThatThrownBy(() -> service.requestReturn(USER_ID, 100L, "Hoa bị héo khi nhận", List.<MultipartFile>of(image(), image())))
                .isInstanceOf(InvalidFileException.class);
        verify(storage).delete("returns/a", MediaType.IMAGE);
        verify(returnRepository, never()).save(any());
    }

    @Test
    void detail_flagsFollowStatusAndDeadline() {
        order.setShop(com.starshop.entity.Shop.builder().id(1L).name("Shop A").slug("shop-a").build());
        order.setCarrier(com.starshop.entity.Carrier.builder().id(1L).name("GHN").build());
        order.setSubtotal(java.math.BigDecimal.TEN);
        order.setTotal(java.math.BigDecimal.TEN);

        var newOrder = service.detail(USER_ID, 100L);
        assertThat(newOrder.isCanCancel()).isTrue();
        assertThat(newOrder.isCanRequestReturn()).isFalse();

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(NOW.minusDays(1));
        var delivered = service.detail(USER_ID, 100L);
        assertThat(delivered.isCanCancel()).isFalse();
        assertThat(delivered.isCanRequestReturn()).isTrue();
        assertThat(delivered.getReturnDeadline()).isEqualTo("08/10/2026 12:00");

        order.setDeliveredAt(NOW.minusDays(4));
        assertThat(service.detail(USER_ID, 100L).isCanRequestReturn()).isFalse();
    }

    private static MockMultipartFile image() {
        return new MockMultipartFile("images", "a.png", "image/png", new byte[]{1, 2, 3});
    }
}
