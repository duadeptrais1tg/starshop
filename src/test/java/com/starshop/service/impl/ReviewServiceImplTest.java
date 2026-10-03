package com.starshop.service.impl;

import com.starshop.dto.review.RatingSummary;
import com.starshop.repository.ReviewMediaRepository;
import com.starshop.repository.ReviewRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewServiceImplTest {

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final ReviewServiceImpl service = new ReviewServiceImpl(reviewRepository, mock(ReviewMediaRepository.class));

    @Test
    void summary_computesAverageAndDistribution() {
        // 3 đánh giá 5 sao, 1 đánh giá 4 sao, 1 đánh giá 1 sao -> (15 + 4 + 1) / 5 = 4.0
        when(reviewRepository.countByRating(1L)).thenReturn(List.of(
                new Object[]{5, 3L}, new Object[]{4, 1L}, new Object[]{1, 1L}));

        RatingSummary summary = service.summary(1L);

        assertThat(summary.getTotal()).isEqualTo(5);
        assertThat(summary.getAverage()).isEqualByComparingTo("4.0");
        assertThat(summary.count(5)).isEqualTo(3);
        assertThat(summary.count(3)).isZero();
        assertThat(summary.percent(5)).isEqualTo(60);
        assertThat(summary.getCountByStar().keySet()).containsExactly(5, 4, 3, 2, 1);
    }

    @Test
    void summary_noReviews_isZero() {
        when(reviewRepository.countByRating(2L)).thenReturn(List.of());

        RatingSummary summary = service.summary(2L);

        assertThat(summary.getTotal()).isZero();
        assertThat(summary.getAverage()).isEqualByComparingTo("0");
        assertThat(summary.percent(5)).isZero();
    }
}
