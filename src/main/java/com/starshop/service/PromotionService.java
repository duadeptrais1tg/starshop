package com.starshop.service;

import com.starshop.dto.OptionDto;
import com.starshop.dto.product.PromotionInfo;
import com.starshop.dto.promotion.AutoPricing;
import com.starshop.dto.promotion.CartLine;
import com.starshop.dto.promotion.CouponDiscount;
import com.starshop.dto.promotion.CouponOption;
import com.starshop.dto.promotion.OrderAutoDiscount;
import com.starshop.dto.promotion.PromotionDto;
import com.starshop.dto.promotion.PromotionForm;
import com.starshop.dto.promotion.PromotionStatus;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

/**
 * Khuyến mãi và mã giảm giá.
 * <ul>
 *   <li>Phần quản lý (Admin): CRUD khuyến mãi toàn sàn / theo danh mục.</li>
 *   <li>Phần cho đặt hàng (A8): {@link #validateCoupon}, {@link #recordUsage}, {@link #releaseUsage}.</li>
 *   <li>Phần hiển thị giá: {@link #autoPricing()}.</li>
 * </ul>
 * Xem docs/khuyen-mai.md.
 */
public interface PromotionService {

    int PAGE_SIZE = 10;

    // ======================================================================= Quản lý (Admin)

    Page<PromotionDto> search(String keyword, PromotionStatus status, int page);

    PromotionForm getForm(Long id);

    /** Đã có người dùng (mã đã được dùng, hoặc khuyến mãi tự áp dụng đã bắt đầu): khóa sửa giá trị, không xóa. */
    boolean isLocked(Long id);

    /** Danh mục đang hiển thị để chọn phạm vi. */
    List<OptionDto> categoryOptions();

    Long create(PromotionForm form, Long createdByUserId);

    /**
     * Khi đã khóa chỉ được sửa tên, mô tả, thời gian kết thúc, số lượt (không nhỏ hơn số đã dùng), giới hạn mỗi người.
     */
    void update(Long id, PromotionForm form);

    void toggleActive(Long id);

    /** @throws com.starshop.exception.BusinessException đã có người dùng */
    void delete(Long id);

    // ======================================================================= Quản lý (Vendor – B8)
    // Khuyến mãi của shop: phạm vi luôn là SHOP của chính vendor; dùng chung kiểm tra và quy tắc khóa với Admin.
    // Khuyến mãi của shop khác -> NotFoundException.

    Page<PromotionDto> searchForShop(Long ownerId, String keyword, PromotionStatus status, int page);

    PromotionForm getShopForm(Long ownerId, Long id);

    boolean isShopLocked(Long ownerId, Long id);

    Long createForShop(Long ownerId, PromotionForm form);

    void updateForShop(Long ownerId, Long id, PromotionForm form);

    void toggleForShop(Long ownerId, Long id);

    /** @throws com.starshop.exception.BusinessException đã có người dùng */
    void deleteForShop(Long ownerId, Long id);

    /** Khuyến mãi đang chạy của shop kèm mã (hiển thị trên trang shop công khai). */
    List<PromotionInfo> activeShopPromotions(Long shopId);

    // ======================================================================= Dùng cho đặt hàng (A8)

    /**
     * Kiểm tra mã giảm giá cho đơn của MỘT shop và tính số tiền giảm. Không thay đổi dữ liệu.
     * Điều kiện: mã và chương trình đang bật, trong thời gian, còn lượt, user chưa dùng quá giới hạn,
     * đúng phạm vi (toàn sàn / danh mục / shop), đủ giá trị đơn tối thiểu.
     *
     * @param lines       các dòng hàng của đơn (giá đã tính ở server)
     * @param shippingFee phí vận chuyển của đơn
     * @throws com.starshop.exception.BusinessException mã không dùng được (message nêu lý do)
     */
    CouponDiscount validateCoupon(String code, Long userId, Long shopId, List<CartLine> lines, BigDecimal shippingFee);

    /**
     * Ghi nhận đã dùng mã khi tạo đơn (gọi trong cùng transaction tạo đơn).
     * Tăng lượt dùng bằng 1 câu UPDATE có điều kiện nên 2 người dùng lượt cuối cùng lúc chỉ 1 người thành công.
     *
     * @throws com.starshop.exception.BusinessException mã vừa hết lượt
     */
    void recordUsage(Long couponId, Long userId, Long orderId, BigDecimal discountAmount);

    /** Trả lại lượt dùng mã của đơn (hủy đơn / thanh toán thất bại). Không có thì bỏ qua. */
    void releaseUsage(Long orderId);

    /**
     * Các mã khách dùng được cho đơn của một shop (mã của shop, theo danh mục và toàn sàn), giảm nhiều nhất trước.
     * Mã chưa đủ điều kiện (đơn tối thiểu, hết lượt, đã dùng hết số lần...) không được liệt kê.
     */
    List<CouponOption> availableCoupons(Long userId, Long shopId, List<CartLine> lines, BigDecimal shippingFee);

    /**
     * Khuyến mãi tự áp dụng ở cấp đơn: giảm % có đơn tối thiểu và giảm phí vận chuyển (không cần mã).
     * Giảm % không có đơn tối thiểu đã nằm trong đơn giá ({@link #autoPricing()}) nên không tính ở đây.
     */
    OrderAutoDiscount autoOrderDiscount(Long shopId, List<CartLine> lines, BigDecimal shippingFee);

    // ======================================================================= Hiển thị giá

    /** Khuyến mãi giảm % tự áp dụng đang chạy (không yêu cầu đơn tối thiểu), để tính giá khuyến mãi trên card. */
    AutoPricing autoPricing();
}
