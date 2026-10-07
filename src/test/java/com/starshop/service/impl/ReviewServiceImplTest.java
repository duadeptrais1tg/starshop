package com.starshop.service.impl;

import com.starshop.dto.UploadResult;
import com.starshop.dto.review.RatingSummary;
import com.starshop.dto.review.ReviewForm;
import com.starshop.entity.Order;
import com.starshop.entity.OrderItem;
import com.starshop.entity.Product;
import com.starshop.entity.Review;
import com.starshop.entity.User;
import com.starshop.entity.enums.MediaType;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.InvalidFileException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.OrderItemRepository;
import com.starshop.repository.ReviewMediaRepository;
import com.starshop.repository.ReviewRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewServiceImplTest {

    private static final Long USER_ID = 7L;
    /** 51 ký tự hiển thị có dấu tiếng Việt (đủ tối thiểu 50). */
    private static final String CONTENT_50 = "Hoa tươi, đóng gói đẹp, giao đúng giờ, rất hài lòng";

    private final ReviewRepository reviewRepository = mock(ReviewRepository.class);
    private final OrderItemRepository orderItemRepository = mock(OrderItemRepository.class);
    private final FileStorageService storage = mock(FileStorageService.class);
    private final ReviewServiceImpl service = new ReviewServiceImpl(reviewRepository, mock(ReviewMediaRepository.class),
            orderItemRepository, mock(UserRepository.class), storage);

    private OrderItem item;

    @BeforeEach
    void setUp() {
        Order order = Order.builder().id(100L).code("SS1").status(OrderStatus.DELIVERED)
                .user(User.builder().id(USER_ID).build()).build();
        item = OrderItem.builder().id(5L).order(order).product(Product.builder().id(10L).slug("hoa").build())
                .productName("Bó hoa").quantity(1).build();
        when(orderItemRepository.findById(5L)).thenReturn(Optional.of(item));
        when(storage.uploadImage(any(), eq("reviews/images")))
                .thenReturn(new UploadResult("https://cdn/i.png", "reviews/images/i", MediaType.IMAGE));
        when(storage.uploadVideo(any(), eq("reviews/videos")))
                .thenReturn(new UploadResult("https://cdn/v.mp4", "reviews/videos/v", MediaType.VIDEO));
    }

    @Test
    void create_savesReview_marksItemReviewed_andRefreshesProductRating() {
        Long orderId = service.create(USER_ID, form(5, "  " + CONTENT_50 + "  "), List.of(image(), image()), video());

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review review = captor.getValue();
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getContent()).isEqualTo(CONTENT_50);
        assertThat(review.getMedia()).extracting(m -> m.getMediaType())
                .containsExactly(MediaType.IMAGE, MediaType.IMAGE, MediaType.VIDEO);
        assertThat(item.isReviewed()).isTrue();
        assertThat(orderId).isEqualTo(100L);
        verify(reviewRepository).refreshProductRating(10L);
    }

    @Test
    void contentLength_countsVisibleCharacters() {
        // 49 ký tự hiển thị -> bị từ chối dù chuỗi UTF-16 có thể dài hơn
        String short49 = CONTENT_50.substring(0, CONTENT_50.offsetByCodePoints(0, 49));
        assertThatThrownBy(() -> service.create(USER_ID, form(4, short49), List.of(), null))
                .hasMessageContaining("tối thiểu 50").hasMessageContaining("49");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void validatesStarsAndMediaLimits() {
        assertThatThrownBy(() -> service.create(USER_ID, form(0, CONTENT_50), List.of(), null)).hasMessageContaining("1 đến 5");
        assertThatThrownBy(() -> service.create(USER_ID, form(6, CONTENT_50), List.of(), null)).hasMessageContaining("1 đến 5");

        List<MultipartFile> six = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            six.add(image());
        }
        assertThatThrownBy(() -> service.create(USER_ID, form(5, CONTENT_50), six, null)).hasMessageContaining("Tối đa 5 ảnh");
        verify(storage, never()).uploadImage(any(), anyString());
    }

    @Test
    void onlyOwnDeliveredAndNotYetReviewedItems() {
        item.getOrder().setUser(User.builder().id(8L).build());
        assertThatThrownBy(() -> service.reviewTarget(USER_ID, 5L)).isInstanceOf(NotFoundException.class);

        item.getOrder().setUser(User.builder().id(USER_ID).build());
        item.getOrder().setStatus(OrderStatus.SHIPPING);
        assertThatThrownBy(() -> service.create(USER_ID, form(5, CONTENT_50), List.of(), null))
                .isInstanceOf(BusinessException.class).hasMessageContaining("đã giao");

        item.getOrder().setStatus(OrderStatus.DELIVERED);
        item.setReviewed(true);
        assertThatThrownBy(() -> service.create(USER_ID, form(5, CONTENT_50), List.of(), null)).hasMessageContaining("đã đánh giá");

        item.setReviewed(false);
        when(reviewRepository.existsByOrderItemId(5L)).thenReturn(true);
        assertThatThrownBy(() -> service.reviewTarget(USER_ID, 5L)).hasMessageContaining("đã đánh giá");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void uploadFails_deletesFilesAlreadyUploaded() {
        when(storage.uploadVideo(any(), eq("reviews/videos"))).thenThrow(new InvalidFileException("Video tối đa 30MB"));

        assertThatThrownBy(() -> service.create(USER_ID, form(5, CONTENT_50), List.of(image()), video()))
                .isInstanceOf(InvalidFileException.class);
        verify(storage).delete("reviews/images/i", MediaType.IMAGE);
        verify(reviewRepository, never()).save(any());
        assertThat(item.isReviewed()).isFalse();
    }

    @Test
    void productReviews_ignoresInvalidStarFilter() {
        when(reviewRepository.search(eq(1L), any(), eq(true), any())).thenReturn(Page.empty());
        service.productReviews(1L, 9, true, 0);
        verify(reviewRepository).search(eq(1L), eq(null), eq(true), any());
        verify(reviewRepository, never()).findByProductIdOrderByCreatedAtDesc(anyLong(), any());
    }

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

    private static ReviewForm form(int rating, String content) {
        ReviewForm form = new ReviewForm();
        form.setOrderItemId(5L);
        form.setRating(rating);
        form.setContent(content);
        return form;
    }

    private static MockMultipartFile image() {
        return new MockMultipartFile("images", "a.png", "image/png", new byte[]{1, 2, 3});
    }

    private static MockMultipartFile video() {
        return new MockMultipartFile("video", "v.mp4", "video/mp4", new byte[]{1, 2, 3});
    }
}
