package com.starshop.service.impl;

import com.starshop.dto.OptionDto;
import com.starshop.dto.promotion.AutoPricing;
import com.starshop.dto.promotion.CartLine;
import com.starshop.dto.promotion.CouponDiscount;
import com.starshop.dto.promotion.CouponOption;
import com.starshop.dto.promotion.OrderAutoDiscount;
import com.starshop.dto.promotion.PromotionDto;
import com.starshop.dto.promotion.PromotionForm;
import com.starshop.dto.promotion.PromotionStatus;
import com.starshop.entity.Category;
import com.starshop.entity.Coupon;
import com.starshop.entity.CouponUsage;
import com.starshop.entity.Promotion;
import com.starshop.entity.enums.PromotionScope;
import com.starshop.entity.enums.PromotionType;
import com.starshop.exception.BusinessException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.CategoryRepository;
import com.starshop.repository.CouponRepository;
import com.starshop.repository.CouponUsageRepository;
import com.starshop.repository.OrderRepository;
import com.starshop.repository.PromotionRepository;
import com.starshop.repository.UserRepository;
import com.starshop.repository.spec.PromotionSpecifications;
import com.starshop.service.PromotionService;
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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal MAX_SHIPPING_DISCOUNT = BigDecimal.valueOf(10_000_000);

    private final PromotionRepository promotionRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final Clock clock;

    // ======================================================================= Quản lý

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionDto> search(String keyword, PromotionStatus status, int page) {
        LocalDateTime now = now();
        Page<Promotion> promotions = promotionRepository.findAll(
                Specification.allOf(PromotionSpecifications.platformManaged(),
                        PromotionSpecifications.nameContains(keyword),
                        PromotionSpecifications.status(status, now)),
                PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by(Sort.Direction.DESC, "id")));

        // Coupon của cả trang lấy bằng 1 query
        Map<Long, Coupon> coupons = new HashMap<>();
        List<Long> ids = promotions.getContent().stream().map(Promotion::getId).toList();
        if (!ids.isEmpty()) {
            for (Coupon c : couponRepository.findByPromotionIdIn(ids)) {
                coupons.putIfAbsent(c.getPromotion().getId(), c);
            }
        }
        return promotions.map(p -> toDto(p, coupons.get(p.getId()), now));
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionForm getForm(Long id) {
        Promotion p = find(id);
        Coupon coupon = couponOf(id);
        PromotionForm form = new PromotionForm();
        form.setName(p.getName());
        form.setDescription(p.getDescription());
        form.setType(p.getType());
        form.setScope(p.getScope());
        form.setCategoryId(p.getCategory() == null ? null : p.getCategory().getId());
        form.setDiscountValue(p.getDiscountValue());
        form.setMaxDiscount(p.getMaxDiscount());
        form.setMinOrderValue(p.getMinOrderValue());
        form.setStartAt(p.getStartAt());
        form.setEndAt(p.getEndAt());
        form.setActive(p.isActive());
        if (coupon != null) {
            form.setCouponCode(coupon.getCode());
            form.setUsageLimit(coupon.getUsageLimit());
            form.setPerUserLimit(coupon.getPerUserLimit());
        }
        return form;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLocked(Long id) {
        return locked(find(id), couponOf(id), now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OptionDto> categoryOptions() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(c -> new OptionDto(c.getId(), c.getName()))
                .toList();
    }

    @Override
    @Transactional
    public Long create(PromotionForm form, Long createdByUserId) {
        validate(form);
        if (!form.getEndAt().isAfter(now())) {
            throw new BusinessException("Thời gian kết thúc phải ở tương lai.");
        }
        String code = normalizeCode(form.getCouponCode());
        if (code != null && couponRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("Mã \"" + code + "\" đã được dùng cho chương trình khác.");
        }
        Promotion p = new Promotion();
        applyValues(p, form);
        p.setName(form.getName().trim());
        p.setDescription(trimToNull(form.getDescription()));
        p.setEndAt(form.getEndAt());
        p.setActive(form.isActive());
        p.setCreatedBy(createdByUserId == null ? null : userRepository.getReferenceById(createdByUserId));
        Promotion saved = promotionRepository.save(p);
        if (code != null) {
            couponRepository.save(Coupon.builder()
                    .promotion(saved).code(code)
                    .usageLimit(form.getUsageLimit()).perUserLimit(form.getPerUserLimit())
                    .build());
        }
        return saved.getId();
    }

    @Override
    @Transactional
    public void update(Long id, PromotionForm form) {
        Promotion p = find(id);
        Coupon coupon = couponOf(id);
        LocalDateTime now = now();
        if (!StringUtils.hasText(form.getName())) {
            throw new BusinessException("Vui lòng nhập tên chương trình.");
        }
        if (form.getEndAt() == null || !form.getEndAt().isAfter(form.getStartAt() == null ? p.getStartAt() : form.getStartAt())) {
            throw new BusinessException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        String code = normalizeCode(form.getCouponCode());

        if (locked(p, coupon, now)) {
            // Đã có người dùng: giá trị đã áp cho khách thì không được đổi nữa
            if (valuesChanged(p, coupon, form, code)) {
                throw new BusinessException("Chương trình đã có người sử dụng nên không thể sửa loại, giá trị, "
                        + "phạm vi, đơn tối thiểu, thời gian bắt đầu hoặc mã. Chỉ sửa được tên, mô tả, "
                        + "thời gian kết thúc và số lượt.");
            }
            // Gia hạn thì luôn được; rút ngắn thì không được về quá khứ; giữ nguyên thì bỏ qua
            if (!form.getEndAt().equals(p.getEndAt())) {
                if (form.getEndAt().isBefore(now) && form.getEndAt().isBefore(p.getEndAt())) {
                    throw new BusinessException("Thời gian kết thúc mới không được ở quá khứ.");
                }
                p.setEndAt(form.getEndAt());
            }
            if (coupon != null) {
                updateLimits(coupon, form);
            }
        } else {
            validate(form);
            if (code != null && couponRepository.existsByCodeIgnoreCaseAndPromotionIdNot(code, id)) {
                throw new BusinessException("Mã \"" + code + "\" đã được dùng cho chương trình khác.");
            }
            applyValues(p, form);
            p.setEndAt(form.getEndAt());
            if (code == null && coupon != null) {
                couponRepository.delete(coupon);
            } else if (code != null && coupon == null) {
                couponRepository.save(Coupon.builder().promotion(p).code(code)
                        .usageLimit(form.getUsageLimit()).perUserLimit(form.getPerUserLimit()).build());
            } else if (code != null) {
                coupon.setCode(code);
                updateLimits(coupon, form);
            }
        }
        p.setName(form.getName().trim());
        p.setDescription(trimToNull(form.getDescription()));
        p.setActive(form.isActive());
    }

    @Override
    @Transactional
    public void toggleActive(Long id) {
        Promotion p = find(id);
        p.setActive(!p.isActive());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Promotion p = find(id);
        Coupon coupon = couponOf(id);
        if (locked(p, coupon, now())) {
            throw new BusinessException("Chương trình đã có người sử dụng nên không thể xóa. Bạn có thể tắt chương trình.");
        }
        couponRepository.deleteAll(couponRepository.findByPromotionId(id));
        promotionRepository.delete(p);
    }

    // ======================================================================= Dùng cho đặt hàng

    @Override
    // Mã không dùng được là kết quả kiểm tra bình thường: không đánh dấu rollback transaction của bên gọi
    // (trang checkout bắt lỗi để hiện lý do rồi vẫn tiếp tục tính tiền)
    @Transactional(readOnly = true, noRollbackFor = BusinessException.class)
    public CouponDiscount validateCoupon(String code, Long userId, Long shopId, List<CartLine> lines, BigDecimal shippingFee) {
        String normalized = normalizeCode(code);
        if (normalized == null) {
            throw new BusinessException("Vui lòng nhập mã giảm giá.");
        }
        Coupon coupon = couponRepository.findByCodeIgnoreCase(normalized)
                .orElseThrow(() -> new BusinessException("Mã giảm giá không tồn tại."));
        Promotion p = coupon.getPromotion();
        LocalDateTime now = now();

        if (!coupon.isActive() || !p.isActive()) {
            throw new BusinessException("Mã giảm giá đã ngừng áp dụng.");
        }
        if (now.isBefore(p.getStartAt())) {
            throw new BusinessException("Mã chưa đến thời gian áp dụng (từ " + DateFormats.dateTime(p.getStartAt()) + ").");
        }
        if (now.isAfter(p.getEndAt())) {
            throw new BusinessException("Mã giảm giá đã hết hạn.");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BusinessException("Mã giảm giá đã hết lượt sử dụng.");
        }
        if (userId != null && couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), userId) >= coupon.getPerUserLimit()) {
            throw new BusinessException("Bạn đã dùng mã này tối đa " + coupon.getPerUserLimit() + " lần.");
        }
        Amounts amounts = evaluate(p, shopId, lines, shippingFee);
        return new CouponDiscount(coupon.getId(), coupon.getCode(), p.getName(), p.getType(),
                amounts.product(), amounts.shipping());
    }

    @Override
    @Transactional
    public void recordUsage(Long couponId, Long userId, Long orderId, BigDecimal discountAmount) {
        if (couponRepository.incrementUsage(couponId) == 0) {
            throw new BusinessException("Mã giảm giá vừa hết lượt sử dụng.");
        }
        couponUsageRepository.save(CouponUsage.builder()
                .coupon(couponRepository.getReferenceById(couponId))
                .user(userRepository.getReferenceById(userId))
                .order(orderRepository.getReferenceById(orderId))
                .discountAmount(discountAmount)
                .build());
    }

    @Override
    @Transactional
    public void releaseUsage(Long orderId) {
        for (CouponUsage usage : couponUsageRepository.findByOrderId(orderId)) {
            couponRepository.decrementUsage(usage.getCoupon().getId());
            couponUsageRepository.delete(usage);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CouponOption> availableCoupons(Long userId, Long shopId, List<CartLine> lines, BigDecimal shippingFee) {
        List<CouponOption> options = new ArrayList<>();
        for (Coupon coupon : couponRepository.findUsableForShop(now(), shopId)) {
            try {
                CouponDiscount d = validateCoupon(coupon.getCode(), userId, shopId, lines, shippingFee);
                Promotion p = coupon.getPromotion();
                options.add(new CouponOption(d.code(), p.getName(), p.getType(), p.getScope(), d.total(),
                        describe(p), p.getEndAt() == null ? null : DateFormats.dateTime(p.getEndAt())));
            } catch (BusinessException e) {
                // Mã không dùng được cho đơn này (chưa đủ đơn tối thiểu, đã dùng hết lượt...): không liệt kê
            }
        }
        options.sort((a, b) -> b.getDiscount().compareTo(a.getDiscount()));
        return options;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderAutoDiscount autoOrderDiscount(Long shopId, List<CartLine> lines, BigDecimal shippingFee) {
        Promotion bestProduct = null;
        Promotion bestShipping = null;
        BigDecimal productDiscount = BigDecimal.ZERO;
        BigDecimal shippingDiscount = BigDecimal.ZERO;
        for (Promotion p : promotionRepository.findRunningAutoPromotions(now())) {
            // Giảm % không có đơn tối thiểu đã nằm sẵn trong đơn giá (autoPricing), không tính lại
            if (p.getType() == PromotionType.PRODUCT_PERCENT && p.getMinOrderValue().signum() == 0) {
                continue;
            }
            Amounts amounts;
            try {
                amounts = evaluate(p, shopId, lines, shippingFee);
            } catch (BusinessException e) {
                continue;
            }
            if (amounts.product().compareTo(productDiscount) > 0) {
                productDiscount = amounts.product();
                bestProduct = p;
            }
            if (amounts.shipping().compareTo(shippingDiscount) > 0) {
                shippingDiscount = amounts.shipping();
                bestShipping = p;
            }
        }
        List<String> names = new ArrayList<>();
        if (bestProduct != null) {
            names.add(bestProduct.getName());
        }
        if (bestShipping != null) {
            names.add(bestShipping.getName());
        }
        return new OrderAutoDiscount(productDiscount, shippingDiscount, names);
    }

    /** Mô tả ngắn cho khách, ví dụ "Giảm 10%, tối đa 50.000₫ – đơn từ 200.000₫". */
    private static String describe(Promotion p) {
        StringBuilder text = new StringBuilder();
        if (p.getType() == PromotionType.PRODUCT_PERCENT) {
            text.append("Giảm ").append(p.getDiscountValue().stripTrailingZeros().toPlainString()).append("%");
            if (p.getMaxDiscount() != null) {
                text.append(", tối đa ").append(money(p.getMaxDiscount())).append("₫");
            }
        } else {
            text.append("Giảm ").append(money(p.getDiscountValue())).append("₫ phí vận chuyển");
        }
        if (p.getMinOrderValue().signum() > 0) {
            text.append(" – đơn từ ").append(money(p.getMinOrderValue())).append("₫");
        }
        return text.toString();
    }

    // ======================================================================= Hiển thị giá

    @Override
    @Transactional(readOnly = true)
    public AutoPricing autoPricing() {
        List<Promotion> running = promotionRepository.findRunningAutoProductPromotions(now());
        if (running.isEmpty()) {
            return AutoPricing.none();
        }
        List<AutoPricing.Rule> rules = new ArrayList<>();
        boolean needCategories = false;
        for (Promotion p : running) {
            needCategories |= p.getScope() == PromotionScope.CATEGORY;
            rules.add(new AutoPricing.Rule(p.getScope(),
                    p.getShop() == null ? null : p.getShop().getId(),
                    p.getCategory() == null ? null : p.getCategory().getId(),
                    p.getDiscountValue(), p.getMaxDiscount()));
        }
        return new AutoPricing(rules, needCategories ? categoryParents() : Map.of());
    }

    // ======================================================================= helpers

    private Promotion find(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy chương trình khuyến mãi #" + id));
    }

    private Coupon couponOf(Long promotionId) {
        return couponRepository.findByPromotionId(promotionId).stream().findFirst().orElse(null);
    }

    /** Có mã: đã có người dùng mã. Không mã (tự áp dụng): chương trình đã bắt đầu. */
    private boolean locked(Promotion p, Coupon coupon, LocalDateTime now) {
        if (coupon != null) {
            return coupon.getUsedCount() > 0 || couponUsageRepository.existsByCouponPromotionId(p.getId());
        }
        return !p.getStartAt().isAfter(now);
    }

    /** Kiểm tra lại toàn bộ giá trị ở service (không chỉ dựa vào @Valid). */
    private void validate(PromotionForm form) {
        if (!StringUtils.hasText(form.getName())) {
            throw new BusinessException("Vui lòng nhập tên chương trình.");
        }
        if (form.getType() == null || form.getScope() == null) {
            throw new BusinessException("Vui lòng chọn loại và phạm vi khuyến mãi.");
        }
        if (form.getScope() == PromotionScope.SHOP) {
            throw new BusinessException("Khuyến mãi theo shop do người bán tự tạo.");
        }
        if (form.getStartAt() == null || form.getEndAt() == null || !form.getEndAt().isAfter(form.getStartAt())) {
            throw new BusinessException("Thời gian kết thúc phải sau thời gian bắt đầu.");
        }
        BigDecimal value = form.getDiscountValue();
        if (value == null || value.signum() <= 0) {
            throw new BusinessException("Giá trị giảm phải lớn hơn 0.");
        }
        if (form.getType() == PromotionType.PRODUCT_PERCENT && value.compareTo(HUNDRED) > 0) {
            throw new BusinessException("Giảm giá sản phẩm tối đa 100%.");
        }
        if (form.getType() == PromotionType.SHIPPING_DISCOUNT && value.compareTo(MAX_SHIPPING_DISCOUNT) > 0) {
            throw new BusinessException("Số tiền giảm phí vận chuyển quá lớn.");
        }
        if (form.getMaxDiscount() != null && form.getMaxDiscount().signum() < 0) {
            throw new BusinessException("Giảm tối đa không được âm.");
        }
        if (form.getMinOrderValue() != null && form.getMinOrderValue().signum() < 0) {
            throw new BusinessException("Đơn tối thiểu không được âm.");
        }
        if (form.getScope() == PromotionScope.CATEGORY) {
            if (form.getCategoryId() == null) {
                throw new BusinessException("Vui lòng chọn danh mục áp dụng.");
            }
            categoryRepository.findById(form.getCategoryId())
                    .filter(Category::isActive)
                    .orElseThrow(() -> new BusinessException("Danh mục không tồn tại hoặc đang ẩn."));
        }
        if (form.getUsageLimit() != null && form.getUsageLimit() < 1) {
            throw new BusinessException("Số lượt phải từ 1 trở lên.");
        }
        if (form.getPerUserLimit() == null || form.getPerUserLimit() < 1) {
            throw new BusinessException("Mỗi người phải được dùng ít nhất 1 lần.");
        }
    }

    /** Các trường "giá trị" (bị khóa khi đã có người dùng). */
    private void applyValues(Promotion p, PromotionForm form) {
        p.setType(form.getType());
        p.setScope(form.getScope());
        p.setCategory(form.getScope() == PromotionScope.CATEGORY ? categoryRepository.getReferenceById(form.getCategoryId()) : null);
        p.setShop(null);
        p.setDiscountValue(form.getDiscountValue());
        // Giảm tối đa chỉ có nghĩa với giảm %
        p.setMaxDiscount(form.getType() == PromotionType.PRODUCT_PERCENT ? form.getMaxDiscount() : null);
        p.setMinOrderValue(form.getMinOrderValue() == null ? BigDecimal.ZERO : form.getMinOrderValue());
        p.setStartAt(form.getStartAt());
    }

    private static boolean valuesChanged(Promotion p, Coupon coupon, PromotionForm form, String code) {
        Long categoryId = p.getCategory() == null ? null : p.getCategory().getId();
        BigDecimal newMax = form.getType() == PromotionType.PRODUCT_PERCENT ? form.getMaxDiscount() : null;
        BigDecimal newMin = form.getMinOrderValue() == null ? BigDecimal.ZERO : form.getMinOrderValue();
        return p.getType() != form.getType()
                || p.getScope() != form.getScope()
                || (form.getScope() == PromotionScope.CATEGORY && !Objects.equals(categoryId, form.getCategoryId()))
                || !sameAmount(p.getDiscountValue(), form.getDiscountValue())
                || !sameAmount(p.getMaxDiscount(), newMax)
                || !sameAmount(p.getMinOrderValue(), newMin)
                || !Objects.equals(p.getStartAt(), form.getStartAt())
                || !Objects.equals(coupon == null ? null : coupon.getCode(), code);
    }

    private static void updateLimits(Coupon coupon, PromotionForm form) {
        if (form.getUsageLimit() != null && form.getUsageLimit() < coupon.getUsedCount()) {
            throw new BusinessException("Số lượt không được nhỏ hơn số lượt đã dùng (" + coupon.getUsedCount() + ").");
        }
        coupon.setUsageLimit(form.getUsageLimit());
        coupon.setPerUserLimit(form.getPerUserLimit());
    }

    private PromotionDto toDto(Promotion p, Coupon coupon, LocalDateTime now) {
        PromotionStatus status;
        if (!p.isActive()) {
            status = PromotionStatus.INACTIVE;
        } else if (now.isBefore(p.getStartAt())) {
            status = PromotionStatus.UPCOMING;
        } else if (now.isAfter(p.getEndAt())) {
            status = PromotionStatus.ENDED;
        } else {
            status = PromotionStatus.RUNNING;
        }
        boolean isLocked = coupon != null ? coupon.getUsedCount() > 0 : !p.getStartAt().isAfter(now);
        return PromotionDto.builder()
                .id(p.getId())
                .name(p.getName())
                .type(p.getType())
                .scope(p.getScope())
                .categoryName(p.getCategory() == null ? null : p.getCategory().getName())
                .discountValue(p.getDiscountValue())
                .maxDiscount(p.getMaxDiscount())
                .minOrderValue(p.getMinOrderValue())
                .startAt(DateFormats.dateTime(p.getStartAt()))
                .endAt(DateFormats.dateTime(p.getEndAt()))
                .status(status)
                .active(p.isActive())
                .couponCode(coupon == null ? null : coupon.getCode())
                .usageLimit(coupon == null ? null : coupon.getUsageLimit())
                .usedCount(coupon == null ? 0 : coupon.getUsedCount())
                .locked(isLocked)
                .build();
    }

    /** Số tiền giảm trên tiền hàng / phí ship của một chương trình cho đơn của một shop. */
    private record Amounts(BigDecimal product, BigDecimal shipping) {
    }

    /**
     * Tính tiền giảm của chương trình cho đơn (đúng phạm vi, đủ đơn tối thiểu).
     *
     * @throws BusinessException không áp dụng được (message nêu lý do)
     */
    private Amounts evaluate(Promotion p, Long shopId, List<CartLine> lines, BigDecimal shippingFee) {
        if (p.getScope() == PromotionScope.SHOP && (p.getShop() == null || !Objects.equals(p.getShop().getId(), shopId))) {
            throw new BusinessException("Mã giảm giá không áp dụng cho shop này.");
        }

        // Số tiền được xét: cả đơn, hoặc chỉ các sản phẩm thuộc danh mục của chương trình
        BigDecimal base = BigDecimal.ZERO;
        Map<Long, Long> parents = p.getScope() == PromotionScope.CATEGORY ? categoryParents() : Map.of();
        for (CartLine line : lines) {
            if (p.getScope() != PromotionScope.CATEGORY
                    || isInCategory(line.categoryId(), p.getCategory().getId(), parents)) {
                base = base.add(line.lineTotal());
            }
        }
        if (base.signum() == 0) {
            throw new BusinessException("Đơn hàng không có sản phẩm thuộc danh mục \"" + p.getCategory().getName() + "\".");
        }
        if (base.compareTo(p.getMinOrderValue()) < 0) {
            throw new BusinessException("Đơn tối thiểu " + money(p.getMinOrderValue()) + "₫ để dùng mã này.");
        }

        BigDecimal productDiscount = BigDecimal.ZERO;
        BigDecimal shippingDiscount = BigDecimal.ZERO;
        if (p.getType() == PromotionType.PRODUCT_PERCENT) {
            productDiscount = cap(base.multiply(p.getDiscountValue()).divide(HUNDRED, 0, RoundingMode.HALF_UP), p.getMaxDiscount())
                    .min(base);
        } else {
            BigDecimal fee = shippingFee == null ? BigDecimal.ZERO : shippingFee;
            shippingDiscount = cap(p.getDiscountValue().min(fee), p.getMaxDiscount());
        }
        return new Amounts(productDiscount, shippingDiscount);
    }

    private Map<Long, Long> categoryParents() {
        Map<Long, Long> parents = new HashMap<>();
        for (Category c : categoryRepository.findAll()) {
            if (c.getParent() != null) {
                parents.put(c.getId(), c.getParent().getId());
            }
        }
        return parents;
    }

    private static boolean isInCategory(Long categoryId, Long promotionCategoryId, Map<Long, Long> parents) {
        Long current = categoryId;
        for (int depth = 0; current != null && depth < 20; depth++) {
            if (current.equals(promotionCategoryId)) {
                return true;
            }
            current = parents.get(current);
        }
        return false;
    }

    private static BigDecimal cap(BigDecimal amount, BigDecimal max) {
        return max != null && amount.compareTo(max) > 0 ? max : amount;
    }

    private static boolean sameAmount(BigDecimal a, BigDecimal b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.compareTo(b) == 0;
    }

    private static String normalizeCode(String code) {
        return StringUtils.hasText(code) ? code.trim().toUpperCase(Locale.ROOT) : null;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String money(BigDecimal value) {
        return String.format(Locale.forLanguageTag("vi-VN"), "%,.0f", value);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
