package com.starshop.dto.review;

import com.starshop.entity.enums.MediaType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Một đánh giá hiển thị ở trang chi tiết sản phẩm (chỉ tên người viết, không lộ email).
 */
@Getter
@Builder
public class ReviewDto {

    private final Long id;
    private final String reviewerName;
    private final int rating;
    private final String content;
    private final String createdAt;
    private final List<Media> media;

    @Getter
    @Builder
    public static class Media {
        private final String url;
        private final MediaType type;

        public boolean isVideo() {
            return type == MediaType.VIDEO;
        }
    }
}
