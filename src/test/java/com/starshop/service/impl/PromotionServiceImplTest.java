package com.starshop.service.impl;

import com.starshop.dto.promotion.CartLine;
import com.starshop.dto.promotion.CouponDiscount;
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
import com.starshop.repository.UserRepository;
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
    private final PromotionServiceImpl service = new PromotionServiceImpl(promotionRepository, couponRepository,
            usageRepository, categoryRepository, mock(UserRepository.class), mock(OrderRepository.class),
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
