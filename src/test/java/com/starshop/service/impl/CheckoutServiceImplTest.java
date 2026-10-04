package com.starshop.service.impl;

import com.starshop.dto.checkout.CheckoutGroup;
import com.starshop.dto.checkout.CheckoutRequest;
import com.starshop.dto.checkout.CheckoutView;
import com.starshop.dto.promotion.AutoPricing;
import com.starshop.dto.promotion.CouponDiscount;
import com.starshop.dto.promotion.OrderAutoDiscount;
import com.starshop.entity.Address;
import com.starshop.entity.Carrier;
import com.starshop.entity.Cart;
import com.starshop.entity.CartItem;
import com.starshop.entity.Category;
import com.starshop.entity.Order;
import com.starshop.entity.Payment;
import com.starshop.entity.Product;
import com.starshop.entity.Shop;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.entity.enums.PaymentMethod;
import com.starshop.entity.enums.PromotionType;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.BusinessException;
import com.starshop.repository.AddressRepository;
import com.starshop.repository.CarrierRepository;
import com.starshop.repository.CartItemRepository;
import com.starshop.repository.CouponRepository;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.OrderStatusHistoryRepository;
import com.starshop.repository.PaymentRepository;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.AddressService;
import com.starshop.service.CommissionService;
import com.starshop.service.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CheckoutServiceImplTest {

    private static final Long USER_ID = 7L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final CartItemRepository cartItemRepository = mock(CartItemRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final AddressRepository addressRepository = mock(AddressRepository.class);
    private final CarrierRepository carrierRepository = mock(CarrierRepository.class);
    private final OrderRepository orderRepository = mock(OrderRepository.class);
    private final OrderStatusHistoryRepository historyRepository = mock(OrderStatusHistoryRepository.class);
    private final PaymentRepository paymentRepository = mock(PaymentRepository.class);
    private final PromotionService promotionService = mock(PromotionService.class);
    private final CommissionService commissionService = mock(CommissionService.class);
    private final CheckoutServiceImpl service = new CheckoutServiceImpl(cartItemRepository, productRepository,
            mock(ProductImageRepository.class), addressRepository, carrierRepository, mock(CouponRepository.class),
            orderRepository, historyRepository, paymentRepository, mock(UserRepository.class), mock(AddressService.class),
            promotionService, commissionService,
            Clock.fixed(LocalDateTime.of(2026, 10, 4, 10, 0).atZone(ZONE).toInstant(), ZONE));

    private Shop shopA;
    private Shop shopB;
    private Product rose;
    private Product lily;
    private Product tulip;
    private List<CartItem> items;
    private Address address;

    @BeforeEach
    void setUp() {
        Category category = Category.builder().id(3L).name("Hoa").active(true).build();
        shopA = Shop.builder().id(1L).name("Shop A").slug("shop-a").status(ShopStatus.APPROVED).build();
        shopB = Shop.builder().id(2L).name("Shop B").slug("shop-b").status(ShopStatus.APPROVED).build();
        rose = product(10L, shopA, category, "200000", 5);
        lily = product(11L, shopA, category, "100000", 5);
        tulip = product(12L, shopB, category, "300000", 1);
        Cart cart = Cart.builder().id(50L).build();
        items = new ArrayList<>(List.of(
                CartItem.builder().id(1L).cart(cart).product(rose).quantity(2).build(),
                CartItem.builder().id(2L).cart(cart).product(lily).quantity(1).build(),
                CartItem.builder().id(3L).cart(cart).product(tulip).quantity(1).build()));
        when(cartItemRepository.findByIdInAndCartUserId(anyList(), eq(USER_ID))).thenAnswer(inv -> items);

        address = Address.builder().id(20L).receiverName("An").phone("0912345678")
                .detail("12 Lê Lợi").ward("Bến Nghé").district("Quận 1").province("TP.HCM").build();
        when(addressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(USER_ID)).thenReturn(List.of(address));
        when(addressRepository.findByIdAndUserId(anyLong(), eq(USER_ID))).thenReturn(Optional.empty());

        Carrier fast = Carrier.builder().id(30L).name("Giao nhanh").shippingFee(new BigDecimal("30000")).active(true).build();
        when(carrierRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(fast));

        when(promotionService.autoPricing()).thenReturn(AutoPricing.none());
        when(promotionService.autoOrderDiscount(anyLong(), anyList(), any()))
                .thenReturn(new OrderAutoDiscount(BigDecimal.ZERO, BigDecimal.ZERO, List.of()));
        when(commissionService.rateFor(anyLong(), any())).thenReturn(new BigDecimal("5"));

        AtomicLong ids = new AtomicLong(100);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId(ids.incrementAndGet());
            return o;
        });
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.decreaseStock(anyLong(), any(Integer.class))).thenReturn(1);
    }

    @Test
    void preview_splitsByShop_andComputesTotalsOnServer() {
        when(promotionService.autoOrderDiscount(eq(2L), anyList(), any()))
                .thenReturn(new OrderAutoDiscount(BigDecimal.ZERO, new BigDecimal("15000"), List.of("Freeship 15k")));
        CheckoutRequest request = request(1L, 2L, 3L);
        request.setCoupons(Map.of(1L, " giam10 "));
        when(promotionService.validateCoupon(eq("GIAM10"), eq(USER_ID), eq(1L), anyList(), any()))
                .thenReturn(new CouponDiscount(9L, "GIAM10", "Giảm 10%", PromotionType.PRODUCT_PERCENT,
                        new BigDecimal("50000"), BigDecimal.ZERO));

        CheckoutView view = service.preview(USER_ID, request);

        assertThat(view.getErrors()).isEmpty();
        assertThat(view.getAddressId()).isEqualTo(20L);
        assertThat(view.getCarrierId()).isEqualTo(30L);
        assertThat(view.getGroups()).hasSize(2);
        CheckoutGroup a = view.getGroups().get(0);
        assertThat(a.getSubtotal()).isEqualByComparingTo("500000");      // 2 x 200k + 100k
        assertThat(a.getTotal()).isEqualByComparingTo("480000");         // - 50k mã + 30k ship
        CheckoutGroup b = view.getGroups().get(1);
        assertThat(b.getTotal()).isEqualByComparingTo("315000");         // 300k + 30k - 15k freeship
        assertThat(view.getTotal()).isEqualByComparingTo("795000");
        assertThat(view.getItemCount()).isEqualTo(4);
    }

    @Test
    void preview_collectsBlockingErrors() {
        when(addressRepository.findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(USER_ID)).thenReturn(List.of());
        tulip.setStock(0);
        CheckoutRequest request = request(1L, 3L);
        request.setCoupons(Map.of(1L, "SAI", 2L, "SAI"));
        doThrow(new BusinessException("Mã giảm giá không tồn tại.")).when(promotionService)
                .validateCoupon(eq("SAI"), any(), anyLong(), anyList(), any());

        CheckoutView view = service.preview(USER_ID, request);

        assertThat(view.isCanPlace()).isFalse();
        assertThat(view.getErrors()).anyMatch(e -> e.contains("địa chỉ"))
                .anyMatch(e -> e.contains("hết hàng"))
                .anyMatch(e -> e.contains("không tồn tại"));
        // Cùng một mã cho 2 đơn trong một lần đặt
        assertThat(view.getGroups().get(1).getCouponError()).contains("một đơn");
    }

    @Test
    void preview_withoutOwnItems_throws() {
        items.clear();
        assertThatThrownBy(() -> service.preview(USER_ID, request(99L))).isInstanceOf(BusinessException.class);
    }

    @Test
    void placeOrder_createsOneOrderPerShop_decreasesStock_recordsCoupon_clearsCart() {
        CheckoutRequest request = request(1L, 2L, 3L);
        request.setCoupons(Map.of(2L, "SHIP"));
        request.setNotes(Map.of(1L, "  Ghi thiệp giúp mình  "));
        when(promotionService.validateCoupon(eq("SHIP"), eq(USER_ID), eq(2L), anyList(), any()))
                .thenReturn(new CouponDiscount(9L, "SHIP", "Freeship", PromotionType.SHIPPING_DISCOUNT,
                        BigDecimal.ZERO, new BigDecimal("30000")));

        String txnRef = service.placeOrder(USER_ID, request);

        ArgumentCaptor<Order> orders = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(2)).save(orders.capture());
        Order a = orders.getAllValues().get(0);
        Order b = orders.getAllValues().get(1);
        assertThat(a.getShop()).isSameAs(shopA);
        assertThat(a.getItems()).hasSize(2);
        assertThat(a.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(a.getTotal()).isEqualByComparingTo("530000");
        assertThat(a.getNote()).isEqualTo("Ghi thiệp giúp mình");
        assertThat(a.getShippingAddress()).isEqualTo("12 Lê Lợi, Bến Nghé, Quận 1, TP.HCM");
        assertThat(a.getCommissionRate()).isEqualByComparingTo("5");
        assertThat(a.getCode()).startsWith("SS261004");
        assertThat(b.getShippingDiscount()).isEqualByComparingTo("30000");
        assertThat(b.getTotal()).isEqualByComparingTo("300000");
        assertThat(a.getPayment()).isSameAs(b.getPayment());
        assertThat(a.getPayment().getAmount()).isEqualByComparingTo("830000");
        assertThat(a.getPayment().getMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(txnRef).isEqualTo(a.getPayment().getTxnRef()).startsWith("PAY");

        verify(productRepository).decreaseStock(10L, 2);
        verify(productRepository).decreaseStock(12L, 1);
        verify(historyRepository, times(2)).save(any());
        verify(cartItemRepository).deleteOrdered(List.of(1L, 2L, 3L), 50L);
        verify(promotionService).recordUsage(9L, USER_ID, b.getId(), new BigDecimal("30000"));
    }

    @Test
    void placeOrder_notEnoughStock_createsNothing() {
        when(productRepository.decreaseStock(12L, 1)).thenReturn(0);

        assertThatThrownBy(() -> service.placeOrder(USER_ID, request(1L, 3L)))
                .isInstanceOf(BusinessException.class).hasMessageContaining("không còn đủ");
        verify(orderRepository, never()).save(any());
        verify(cartItemRepository, never()).deleteOrdered(anyList(), anyLong());
    }

    @Test
    void placeOrder_withBlockingError_orUnsupportedPayment_createsNothing() {
        CheckoutRequest request = request(1L);
        request.setPaymentMethod(PaymentMethod.VNPAY);
        assertThatThrownBy(() -> service.placeOrder(USER_ID, request)).hasMessageContaining("sẽ sớm được hỗ trợ");

        rose.setActive(false);
        assertThatThrownBy(() -> service.placeOrder(USER_ID, request(1L))).hasMessageContaining("ngừng bán");
        verify(productRepository, never()).decreaseStock(anyLong(), any(Integer.class));
        verify(orderRepository, never()).save(any());
        verify(promotionService, never()).recordUsage(anyLong(), anyLong(), anyLong(), any());
        verify(paymentRepository, never()).existsByTxnRef(anyString());
    }

    private static CheckoutRequest request(Long... itemIds) {
        CheckoutRequest request = new CheckoutRequest();
        request.setItemIds(new ArrayList<>(List.of(itemIds)));
        return request;
    }

    private static Product product(Long id, Shop shop, Category category, String price, int stock) {
        return Product.builder().id(id).shop(shop).category(category).name("Hoa " + id).slug("hoa-" + id)
                .price(new BigDecimal(price)).stock(stock).active(true).build();
    }
}
