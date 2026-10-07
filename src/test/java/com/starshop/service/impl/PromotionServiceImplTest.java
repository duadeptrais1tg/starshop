package com.starshop.service.impl;

import com.starshop.dto.promotion.CartLine;
import com.starshop.dto.promotion.CouponDiscount;
import com.starshop.dto.promotion.CouponOption;
import com.starshop.dto.promotion.OrderAutoDiscount;
import com.starshop.dto.promotion.PromotionForm;
import com.starshop.entity.Category;
import com.starshop.entity.Coupon;
import com.starshop.entity.CouponUsage;
import com.starshop.entity.Promotion;
import com.starshop.entity.Shop;
import com.starshop.entity.enums.PromotionScope;
import com.starshop.entity.enums.PromotionType;
import com.starshop.exception.BusinessException;
import com.starshop.repository.CategoryRepository;
import com.starshop.repository.CouponRepository;
import com.starshop.repository.CouponUsageRepository;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.PromotionRepository;
import com.starshop.repository.ShopRepository;
import com.starshop.repository.UserRepository;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.NotFoundException;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromotionServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 12, 0);

    private final PromotionRepository promotionRepository = mock(PromotionRepository.class);
    private final CouponRepository couponRepository = mock(CouponRepository.class);
    private final CouponUsageRepository usageRepository = mock(CouponUsageRepository.class);
    private final CategoryRepository categoryRepository = mock(CategoryRepository.class);
    private final ShopRepository shopRepository = mock(ShopRepository.class);
    private final PromotionServiceImpl service = new PromotionServiceImpl(promotionRepository, couponRepository,
            usageRepository, categoryRepository, mock(UserRepository.class), mock(OrderRepository.class),
            shopRepository,
            Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));

    private Category flowers;
    private Category roses;

    @BeforeEach
    void setUp() {
        flowers = category(1L, "Hoa tình yêu", null);
        roses = category(2L, "Hoa hồng", flowers);
        when(categoryRepository.findAll()).thenReturn(List.of(flowers, roses));
    }

    // ======================================================= validateCoupon (dùng cho A8)

    @Test
    void percentCoupon_isCappedByMaxDiscount() {
        coupon("GIAM20", promotion(PromotionType.PRODUCT_PERCENT, "20", "50000", "0"), null);

        CouponDiscount d = service.validateCoupon(" giam20 ", 5L, 1L, List.of(line(3L, "400000")), money("30000"));

        assertThat(d.code()).isEqualTo("GIAM20");
        assertThat(d.productDiscount()).isEqualByComparingTo("50000");  // 20% của 400k = 80k > tối đa 50k
        assertThat(d.shippingDiscount()).isZero();
    }

    @Test
    void shippingCoupon_cannotExceedShippingFee() {
        coupon("FREESHIP", promotion(PromotionType.SHIPPING_DISCOUNT, "40000", null, "0"), null);

        CouponDiscount d = service.validateCoupon("FREESHIP", 5L, 1L, List.of(line(3L, "200000")), money("30000"));

        assertThat(d.shippingDiscount()).isEqualByComparingTo("30000");
        assertThat(d.productDiscount()).isZero();
    }

    @Test
    void categoryCoupon_onlyCountsItemsInCategoryOrItsChildren() {
        Promotion p = promotion(PromotionType.PRODUCT_PERCENT, "10", null, "0");
        p.setScope(PromotionScope.CATEGORY);
        p.setCategory(flowers);
        coupon("TINHYEU", p, null);

        // 300k hoa hồng (danh mục con) + 500k hoa khác -> chỉ tính 300k
        CouponDiscount d = service.validateCoupon("TINHYEU", 5L, 1L,
                List.of(line(2L, "300000"), line(9L, "500000")), money("30000"));
        assertThat(d.productDiscount()).isEqualByComparingTo("30000");

        assertThatThrownBy(() -> service.validateCoupon("TINHYEU", 5L, 1L, List.of(line(9L, "500000")), money("0")))
                .hasMessageContaining("không có sản phẩm thuộc danh mục");
    }

    @Test
    void minimumOrder_expiry_inactive_unknown_areRejectedWithReason() {
        Promotion p = promotion(PromotionType.PRODUCT_PERCENT, "10", null, "500000");
        coupon("DON500", p, null);
        assertThatThrownBy(() -> service.validateCoupon("DON500", 5L, 1L, List.of(line(3L, "300000")), money("0")))
                .hasMessageContaining("Đơn tối thiểu 500.000");

        p.setMinOrderValue(BigDecimal.ZERO);
        p.setEndAt(NOW.minusMinutes(1));
        assertThatThrownBy(() -> service.validateCoupon("DON500", 5L, 1L, List.of(line(3L, "300000")), money("0")))
                .hasMessageContaining("hết hạn");

        p.setEndAt(NOW.plusDays(1));
        p.setActive(false);
        assertThatThrownBy(() -> service.validateCoupon("DON500", 5L, 1L, List.of(line(3L, "300000")), money("0")))
                .hasMessageContaining("ngừng áp dụng");

        assertThatThrownBy(() -> service.validateCoupon("KHONGCO", 5L, 1L, List.of(line(3L, "300000")), money("0")))
                .hasMessageContaining("không tồn tại");
    }

    @Test
    void usageLimits_areEnforced() {
        Coupon c = coupon("HET", promotion(PromotionType.PRODUCT_PERCENT, "10", null, "0"), 5);
        c.setUsedCount(5);
        assertThatThrownBy(() -> service.validateCoupon("HET", 5L, 1L, List.of(line(3L, "300000")), money("0")))
                .hasMessageContaining("hết lượt");

        c.setUsedCount(1);
        when(usageRepository.countByCouponIdAndUserId(c.getId(), 5L)).thenReturn(1L);
        assertThatThrownBy(() -> service.validateCoupon("HET", 5L, 1L, List.of(line(3L, "300000")), money("0")))
                .hasMessageContaining("tối đa 1 lần");
    }

    @Test
    void shopCoupon_onlyForThatShop() {
        Promotion p = promotion(PromotionType.PRODUCT_PERCENT, "10", null, "0");
        p.setScope(PromotionScope.SHOP);
        Shop shop = Shop.builder().name("Shop A").build();
        shop.setId(7L);
        p.setShop(shop);
        coupon("SHOPA", p, null);

        assertThatThrownBy(() -> service.validateCoupon("SHOPA", 5L, 8L, List.of(line(3L, "300000")), money("0")))
                .hasMessageContaining("không áp dụng cho shop này");
        assertThat(service.validateCoupon("SHOPA", 5L, 7L, List.of(line(3L, "300000")), money("0")).productDiscount())
                .isEqualByComparingTo("30000");
    }

    // ======================================================= ghi / trả lượt dùng

    @Test
    void validateCoupon_doesNotMarkCallerTransactionRollbackOnly() throws Exception {
        // Checkout bắt lỗi mã để hiện lý do; nếu bị đánh dấu rollback-only thì cả trang lỗi 500
        var tx = PromotionServiceImpl.class.getMethod("validateCoupon", String.class, Long.class, Long.class,
                List.class, BigDecimal.class).getAnnotation(org.springframework.transaction.annotation.Transactional.class);
        assertThat(tx.noRollbackFor()).contains(BusinessException.class);
    }

    @Test
    void availableCoupons_listsOnlyUsableCodes_biggestDiscountFirst() {
        Coupon small = coupon("GIAM5", promotion(PromotionType.PRODUCT_PERCENT, "5", null, "0"), null);
        Coupon big = coupon("GIAM10", promotion(PromotionType.PRODUCT_PERCENT, "10", null, "0"), null);
        big.setId(11L);
        Coupon tooBig = coupon("DON1TR", promotion(PromotionType.PRODUCT_PERCENT, "20", null, "1000000"), null);
        tooBig.setId(12L);
        when(couponRepository.findUsableForShop(NOW, 1L)).thenReturn(List.of(small, big, tooBig));

        List<CouponOption> options = service.availableCoupons(5L, 1L, List.of(line(3L, "400000")), money("30000"));

        assertThat(options).extracting(CouponOption::getCode).containsExactly("GIAM10", "GIAM5");
        assertThat(options.get(0).getDiscount()).isEqualByComparingTo("40000");
        assertThat(options.get(0).getDescription()).isEqualTo("Giảm 10%");
        assertThat(small.getId()).isEqualTo(10L);
    }

    @Test
    void autoOrderDiscount_appliesBestShippingAndMinOrderPercent_skipsPriceLevelPromotions() {
        Promotion freeship = promotion(PromotionType.SHIPPING_DISCOUNT, "20000", null, "300000");
        Promotion bigFreeship = promotion(PromotionType.SHIPPING_DISCOUNT, "50000", null, "300000");
        bigFreeship.setName("Freeship lớn");
        Promotion percentMin = promotion(PromotionType.PRODUCT_PERCENT, "10", null, "200000");
        percentMin.setName("Giảm 10% đơn 200k");
        // Đã nằm trong đơn giá (autoPricing) -> không tính lần nữa
        Promotion priceLevel = promotion(PromotionType.PRODUCT_PERCENT, "50", null, "0");
        Promotion notReached = promotion(PromotionType.PRODUCT_PERCENT, "30", null, "900000");
        when(promotionRepository.findRunningAutoPromotions(NOW))
                .thenReturn(List.of(freeship, bigFreeship, percentMin, priceLevel, notReached));

        OrderAutoDiscount d = service.autoOrderDiscount(1L, List.of(line(3L, "400000")), money("30000"));

        assertThat(d.productDiscount()).isEqualByComparingTo("40000");
        assertThat(d.shippingDiscount()).isEqualByComparingTo("30000");   // không vượt phí ship
        assertThat(d.promotionNames()).containsExactly("Giảm 10% đơn 200k", "Freeship lớn");
    }

    @Test
    void recordUsage_failsWhenLastUseWasJustTaken() {
        when(couponRepository.incrementUsage(10L)).thenReturn(0);

        assertThatThrownBy(() -> service.recordUsage(10L, 5L, 99L, money("1000")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("vừa hết lượt");
        verify(usageRepository, never()).save(any());
    }

    @Test
    void releaseUsage_givesBackTheUse() {
        Coupon c = coupon("X", promotion(PromotionType.PRODUCT_PERCENT, "10", null, "0"), null);
        CouponUsage usage = CouponUsage.builder().coupon(c).build();
        when(usageRepository.findByOrderId(99L)).thenReturn(List.of(usage));

        service.releaseUsage(99L);

        verify(couponRepository).decrementUsage(c.getId());
        verify(usageRepository).delete(usage);
    }

    // ======================================================= khóa khi đã có người dùng

    @Test
    void usedCoupon_locksValueChanges_butAllowsNameAndEndDate() {
        Promotion p = promotion(PromotionType.PRODUCT_PERCENT, "10", null, "0");
        p.setId(3L);
        Coupon c = coupon("DADUNG", p, 100);
        c.setUsedCount(4);
        when(promotionRepository.findById(3L)).thenReturn(Optional.of(p));
        when(couponRepository.findByPromotionId(3L)).thenReturn(List.of(c));

        PromotionForm form = formOf(p, "DADUNG");
        form.setDiscountValue(money("50"));
        assertThatThrownBy(() -> service.update(3L, form)).hasMessageContaining("đã có người sử dụng");
        assertThat(p.getDiscountValue()).isEqualByComparingTo("10");

        PromotionForm ok = formOf(p, "DADUNG");
        ok.setName("Tên mới");
        ok.setEndAt(p.getEndAt().plusDays(3));
        ok.setUsageLimit(200);
        service.update(3L, ok);
        assertThat(p.getName()).isEqualTo("Tên mới");
        assertThat(c.getUsageLimit()).isEqualTo(200);

        PromotionForm tooLow = formOf(p, "DADUNG");
        tooLow.setUsageLimit(2);
        assertThatThrownBy(() -> service.update(3L, tooLow)).hasMessageContaining("nhỏ hơn số lượt đã dùng");

        assertThatThrownBy(() -> service.delete(3L)).hasMessageContaining("không thể xóa");
    }

    @Test
    void create_validatesValuesAndDates() {
        PromotionForm f = new PromotionForm();
        f.setName("Test");
        f.setType(PromotionType.PRODUCT_PERCENT);
        f.setScope(PromotionScope.PLATFORM);
        f.setDiscountValue(money("120"));
        f.setStartAt(NOW.plusDays(1));
        f.setEndAt(NOW.plusDays(2));
        assertThatThrownBy(() -> service.create(f, 1L)).hasMessageContaining("tối đa 100%");

        f.setDiscountValue(money("10"));
        f.setEndAt(NOW.plusHours(1));
        assertThatThrownBy(() -> service.create(f, 1L)).hasMessageContaining("sau thời gian bắt đầu");

        f.setEndAt(NOW.plusDays(2));
        f.setScope(PromotionScope.CATEGORY);
        assertThatThrownBy(() -> service.create(f, 1L)).hasMessageContaining("chọn danh mục");
    }

    // ---------------------------------------------------------------- helpers

    // ======================================================= Khuyến mãi của shop (B8)

    @Test
    void vendorCreate_alwaysShopScope_ofOwnShop() {
        Shop shop = shopOf(3L, 7L);
        when(promotionRepository.save(any(Promotion.class))).thenAnswer(inv -> {
            Promotion p = inv.getArgument(0);
            p.setId(50L);
            return p;
        });
        PromotionForm form = formOf(promotion(PromotionType.PRODUCT_PERCENT, "10", null, "0"), "SHOP10");
        form.setScope(PromotionScope.PLATFORM);   // vendor cố gửi phạm vi toàn sàn
        form.setCategoryId(1L);

        service.createForShop(3L, form);

        ArgumentCaptor<Promotion> captor = ArgumentCaptor.forClass(Promotion.class);
        verify(promotionRepository).save(captor.capture());
        assertThat(captor.getValue().getScope()).isEqualTo(PromotionScope.SHOP);
        assertThat(captor.getValue().getShop()).isSameAs(shop);
        assertThat(captor.getValue().getCategory()).isNull();
        verify(couponRepository).save(any(Coupon.class));
    }

    @Test
    void vendor_cannotTouchOtherShopsOrPlatformPromotions() {
        shopOf(3L, 7L);
        Promotion otherShop = promotion(PromotionType.PRODUCT_PERCENT, "10", null, "0");
        otherShop.setId(60L);
        otherShop.setScope(PromotionScope.SHOP);
        otherShop.setShop(Shop.builder().id(8L).build());
        Promotion platform = promotion(PromotionType.PRODUCT_PERCENT, "10", null, "0");
        platform.setId(61L);
        when(promotionRepository.findById(60L)).thenReturn(Optional.of(otherShop));
        when(promotionRepository.findById(61L)).thenReturn(Optional.of(platform));

        for (Long id : List.of(60L, 61L)) {
            assertThatThrownBy(() -> service.getShopForm(3L, id)).isInstanceOf(NotFoundException.class);
            assertThatThrownBy(() -> service.toggleForShop(3L, id)).isInstanceOf(NotFoundException.class);
            assertThatThrownBy(() -> service.deleteForShop(3L, id)).isInstanceOf(NotFoundException.class);
            assertThatThrownBy(() -> service.updateForShop(3L, id, formOf(otherShop, null))).isInstanceOf(NotFoundException.class);
        }
        assertThat(otherShop.isActive()).isTrue();
        verify(promotionRepository, never()).delete(any(Promotion.class));
    }

    @Test
    void vendorWithoutApprovedShop_cannotCreate() {
        Shop pending = shopOf(3L, 7L);
        pending.setStatus(ShopStatus.PENDING);
        assertThatThrownBy(() -> service.createForShop(3L, formOf(promotion(PromotionType.PRODUCT_PERCENT, "10", null, "0"), null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void vendor_sharesValidationAndLockRules() {
        Shop shop = shopOf(3L, 7L);
        Promotion own = promotion(PromotionType.PRODUCT_PERCENT, "10", null, "0");
        own.setId(70L);
        own.setScope(PromotionScope.SHOP);
        own.setShop(shop);
        when(promotionRepository.findById(70L)).thenReturn(Optional.of(own));
        Coupon coupon = coupon("SHOP10", own, 10);
        coupon.setUsedCount(1);   // đã có người dùng -> khóa giá trị
        when(couponRepository.findByPromotionId(70L)).thenReturn(List.of(coupon));

        PromotionForm tooMuch = formOf(own, "SHOP10");
        tooMuch.setDiscountValue(new BigDecimal("150"));
        assertThatThrownBy(() -> service.updateForShop(3L, 70L, tooMuch)).hasMessageContaining("không thể sửa");
        assertThat(service.isShopLocked(3L, 70L)).isTrue();
        assertThatThrownBy(() -> service.deleteForShop(3L, 70L)).hasMessageContaining("không thể xóa");

        PromotionForm fresh = formOf(own, null);
        fresh.setDiscountValue(new BigDecimal("150"));
        assertThatThrownBy(() -> service.createForShop(3L, fresh)).hasMessageContaining("tối đa 100%");
    }

    @Test
    void admin_stillCannotCreateShopScope() {
        PromotionForm form = formOf(promotion(PromotionType.PRODUCT_PERCENT, "10", null, "0"), null);
        form.setScope(PromotionScope.SHOP);
        assertThatThrownBy(() -> service.create(form, 1L)).hasMessageContaining("người bán tự tạo");
    }

    @Test
    void activeShopPromotions_includeCouponCodes() {
        Promotion p = promotion(PromotionType.SHIPPING_DISCOUNT, "20000", null, "0");
        p.setId(80L);
        p.setName("Freeship shop");
        when(promotionRepository.findRunningForShop(3L, NOW)).thenReturn(List.of(p));
        Coupon c = Coupon.builder().code("FREESHIP").promotion(p).build();
        when(couponRepository.findByPromotionIdInAndActiveTrue(List.of(80L))).thenReturn(List.of(c));

        var infos = service.activeShopPromotions(3L);

        assertThat(infos).hasSize(1);
        assertThat(infos.get(0).getName()).isEqualTo("Freeship shop");
        assertThat(infos.get(0).getCouponCodes()).containsExactly("FREESHIP");
    }

    private Shop shopOf(Long ownerId, Long shopId) {
        Shop shop = Shop.builder().id(shopId).name("Shop A").status(ShopStatus.APPROVED).build();
        when(shopRepository.findByOwnerId(ownerId)).thenReturn(Optional.of(shop));
        return shop;
    }

    private Promotion promotion(PromotionType type, String value, String max, String min) {
        return Promotion.builder()
                .name("KM").type(type).scope(PromotionScope.PLATFORM)
                .discountValue(money(value)).maxDiscount(max == null ? null : money(max)).minOrderValue(money(min))
                .startAt(NOW.minusDays(1)).endAt(NOW.plusDays(5)).active(true)
                .build();
    }

    private Coupon coupon(String code, Promotion promotion, Integer usageLimit) {
        Coupon c = Coupon.builder().code(code).promotion(promotion).usageLimit(usageLimit).build();
        c.setId(10L);
        when(couponRepository.findByCodeIgnoreCase(code)).thenReturn(Optional.of(c));
        return c;
    }

    private static PromotionForm formOf(Promotion p, String code) {
        PromotionForm f = new PromotionForm();
        f.setName(p.getName());
        f.setType(p.getType());
        f.setScope(p.getScope());
        f.setDiscountValue(p.getDiscountValue());
        f.setMaxDiscount(p.getMaxDiscount());
        f.setMinOrderValue(p.getMinOrderValue());
        f.setStartAt(p.getStartAt());
        f.setEndAt(p.getEndAt());
        f.setCouponCode(code);
        f.setUsageLimit(100);
        f.setPerUserLimit(1);
        f.setActive(true);
        return f;
    }

    private static Category category(Long id, String name, Category parent) {
        Category c = Category.builder().name(name).slug("c" + id).parent(parent).build();
        c.setId(id);
        return c;
    }

    private static CartLine line(Long categoryId, String total) {
        return new CartLine(categoryId, money(total));
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }
}
