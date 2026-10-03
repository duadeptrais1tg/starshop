package com.starshop.service.impl;

import com.starshop.dto.review.RatingSummary;
import com.starshop.dto.review.ReviewDto;
import com.starshop.entity.Review;
import com.starshop.entity.ReviewMedia;
import com.starshop.repository.ReviewMediaRepository;
import com.starshop.repository.ReviewRepository;
import com.starshop.service.ReviewService;
import com.starshop.util.DateFormats;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;

    @Override
    public Page<ReviewDto> productReviews(Long productId, int page) {
        Page<Review> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(
                productId, PageRequest.of(Math.max(page, 0), PAGE_SIZE));

        // Media của cả trang đánh giá lấy bằng 1 query
        Map<Long, List<ReviewDto.Media>> mediaByReview = new HashMap<>();
        List<Long> ids = reviews.getContent().stream().map(Review::getId).toList();
        if (!ids.isEmpty()) {
            for (ReviewMedia m : reviewMediaRepository.findByReviewIdInOrderByIdAsc(ids)) {
                mediaByReview.computeIfAbsent(m.getReview().getId(), k -> new ArrayList<>())
                        .add(ReviewDto.Media.builder().url(m.getUrl()).type(m.getMediaType()).build());
            }
        }
        return reviews.map(r -> ReviewDto.builder()
                .id(r.getId())
                .reviewerName(r.getUser().getFullName())
                .rating(r.getRating())
                .content(r.getContent())
                .createdAt(DateFormats.dateTime(r.getCreatedAt()))
                .media(mediaByReview.getOrDefault(r.getId(), List.of()))
                .build());
    }

    @Override
    public RatingSummary summary(Long productId) {
        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            counts.put(star, 0L);
        }
        long total = 0;
        long sum = 0;
        for (Object[] row : reviewRepository.countByRating(productId)) {
            int star = ((Number) row[0]).intValue();
            long count = (Long) row[1];
            counts.put(star, count);
            total += count;
            sum += star * count;
        }
        BigDecimal average = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP);
        return new RatingSummary(average, total, counts);
    }
}
