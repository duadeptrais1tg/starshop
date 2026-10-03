package com.starshop.service;

import com.starshop.dto.review.RatingSummary;
import com.starshop.dto.review.ReviewDto;
import org.springframework.data.domain.Page;

/**
 * Đánh giá sản phẩm. Phần đọc (hiển thị ở trang chi tiết); phần viết đánh giá bổ sung ở chức năng đánh giá.
 */
public interface ReviewService {

    int PAGE_SIZE = 5;

    /** @param page bắt đầu từ 0 */
    Page<ReviewDto> productReviews(Long productId, int page);

    RatingSummary summary(Long productId);
}
