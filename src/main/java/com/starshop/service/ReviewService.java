package com.starshop.service;

import com.starshop.dto.review.RatingSummary;
import com.starshop.dto.review.ReviewDto;
import com.starshop.dto.review.ReviewForm;
import com.starshop.dto.review.ReviewTarget;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Đánh giá sản phẩm: hiển thị ở trang chi tiết và khách viết đánh giá cho sản phẩm đã mua.
 */
public interface ReviewService {

    int PAGE_SIZE = 5;
    int MAX_IMAGES = 5;

    /** @param page bắt đầu từ 0 */
    Page<ReviewDto> productReviews(Long productId, int page);

    /**
     * @param star      lọc theo số sao (null = tất cả)
     * @param withMedia chỉ đánh giá có ảnh / video
     * @param page      bắt đầu từ 0
     */
    Page<ReviewDto> productReviews(Long productId, Integer star, boolean withMedia, int page);

    RatingSummary summary(Long productId);

    long countWithMedia(Long productId);

    /**
     * Sản phẩm được phép đánh giá: thuộc đơn của chính user, đơn đã giao, chưa đánh giá.
     *
     * @throws com.starshop.exception.NotFoundException  không phải đơn của user
     * @throws com.starshop.exception.BusinessException đơn chưa giao / đã đánh giá
     */
    ReviewTarget reviewTarget(Long userId, Long orderItemId);

    /**
     * Lưu đánh giá (1–5 sao, nội dung ≥ 50 ký tự, tối đa 5 ảnh + 1 video) và cập nhật điểm trung bình sản phẩm.
     *
     * @return id đơn hàng (để quay lại trang đơn)
     */
    Long create(Long userId, ReviewForm form, List<MultipartFile> images, MultipartFile video);
}
