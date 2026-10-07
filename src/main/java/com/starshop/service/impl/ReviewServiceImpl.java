package com.starshop.service.impl;

import com.starshop.dto.UploadResult;
import com.starshop.dto.review.RatingSummary;
import com.starshop.dto.review.ReviewDto;
import com.starshop.dto.review.ReviewForm;
import com.starshop.dto.review.ReviewTarget;
import com.starshop.entity.Order;
import com.starshop.entity.OrderItem;
import com.starshop.entity.Review;
import com.starshop.entity.ReviewMedia;
import com.starshop.entity.enums.MediaType;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.exception.BusinessException;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.OrderItemRepository;
import com.starshop.repository.ReviewMediaRepository;
import com.starshop.repository.ReviewRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.FileStorageService;
import com.starshop.service.ReviewService;
import com.starshop.util.DateFormats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private static final String IMAGE_FOLDER = "reviews/images";
    private static final String VIDEO_FOLDER = "reviews/videos";

    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    @Override
    public Page<ReviewDto> productReviews(Long productId, int page) {
        return productReviews(productId, null, false, page);
    }

    @Override
    public Page<ReviewDto> productReviews(Long productId, Integer star, boolean withMedia, int page) {
        Integer validStar = star != null && star >= 1 && star <= 5 ? star : null;
        Page<Review> reviews = reviewRepository.search(productId, validStar, withMedia,
                PageRequest.of(Math.max(page, 0), PAGE_SIZE));

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

    @Override
    public long countWithMedia(Long productId) {
        return reviewRepository.countWithMedia(productId);
    }

    @Override
    public ReviewTarget reviewTarget(Long userId, Long orderItemId) {
        OrderItem item = requireReviewable(userId, orderItemId);
        Order order = item.getOrder();
        return ReviewTarget.builder()
                .orderItemId(item.getId())
                .orderId(order.getId())
                .orderCode(order.getCode())
                .productName(item.getProductName())
                .productSlug(item.getProduct().getSlug())
                .imageUrl(item.getProductImage())
                .quantity(item.getQuantity())
                .build();
    }

    @Override
    @Transactional
    public Long create(Long userId, ReviewForm form, List<MultipartFile> images, MultipartFile video) {
        OrderItem item = requireReviewable(userId, form.getOrderItemId());
        if (form.getRating() == null || form.getRating() < 1 || form.getRating() > 5) {
            throw new BusinessException("Vui lòng chọn từ 1 đến 5 sao.");
        }
        String content = form.getContent() == null ? "" : form.getContent().trim();
        int length = content.codePointCount(0, content.length());
        if (length < ReviewForm.MIN_CONTENT) {
            throw new BusinessException("Nội dung đánh giá tối thiểu " + ReviewForm.MIN_CONTENT
                    + " ký tự (hiện có " + length + ").");
        }
        if (length > ReviewForm.MAX_CONTENT) {
            throw new BusinessException("Nội dung đánh giá tối đa " + ReviewForm.MAX_CONTENT + " ký tự.");
        }
        List<MultipartFile> imageFiles = images == null ? List.of()
                : images.stream().filter(f -> f != null && !f.isEmpty()).toList();
        if (imageFiles.size() > MAX_IMAGES) {
            throw new BusinessException("Tối đa " + MAX_IMAGES + " ảnh.");
        }
        MultipartFile videoFile = video == null || video.isEmpty() ? null : video;

        Review review = Review.builder()
                .user(userRepository.getReferenceById(userId))
                .product(item.getProduct())
                .orderItem(item)
                .rating(form.getRating())
                .content(content)
                .build();
        uploadMedia(review, imageFiles, videoFile);
        reviewRepository.save(review);
        item.setReviewed(true);
        reviewRepository.refreshProductRating(item.getProduct().getId());
        return item.getOrder().getId();
    }

    // ------------------------------------------------------------------ helpers

    /** Dòng đơn của chính user, đơn đã giao, chưa đánh giá. Của người khác coi như không tồn tại. */
    private OrderItem requireReviewable(Long userId, Long orderItemId) {
        OrderItem item = orderItemId == null ? null : orderItemRepository.findById(orderItemId)
                .filter(i -> i.getOrder().getUser().getId().equals(userId))
                .orElse(null);
        if (item == null) {
            throw new NotFoundException("Không tìm thấy sản phẩm trong đơn hàng của bạn");
        }
        if (item.getOrder().getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException("Chỉ đánh giá được sản phẩm trong đơn đã giao.");
        }
        if (item.isReviewed() || reviewRepository.existsByOrderItemId(item.getId())) {
            throw new BusinessException("Bạn đã đánh giá sản phẩm này trong đơn " + item.getOrder().getCode() + ".");
        }
        return item;
    }

    /**
     * Upload ảnh / video và gắn vào đánh giá. Lỗi giữa chừng -> xóa file đã upload;
     * transaction rollback sau đó cũng xóa để không bỏ rác trên Cloudinary.
     */
    private void uploadMedia(Review review, List<MultipartFile> images, MultipartFile video) {
        List<UploadResult> uploaded = new ArrayList<>();
        try {
            for (MultipartFile image : images) {
                uploaded.add(fileStorageService.uploadImage(image, IMAGE_FOLDER));
            }
            if (video != null) {
                uploaded.add(fileStorageService.uploadVideo(video, VIDEO_FOLDER));
            }
        } catch (RuntimeException e) {
            uploaded.forEach(this::deleteQuietly);
            throw e;
        }
        for (UploadResult result : uploaded) {
            review.addMedia(ReviewMedia.builder()
                    .url(result.url()).publicId(result.publicId()).mediaType(result.mediaType()).build());
        }
        if (!uploaded.isEmpty() && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) {
                        uploaded.forEach(ReviewServiceImpl.this::deleteQuietly);
                    }
                }
            });
        }
    }

    private void deleteQuietly(UploadResult file) {
        if (file == null || !StringUtils.hasText(file.publicId())) {
            return;
        }
        try {
            fileStorageService.delete(file.publicId(), file.mediaType() == null ? MediaType.IMAGE : file.mediaType());
        } catch (FileStorageException e) {
            log.warn("Không xóa được file đánh giá {}: {}", file.publicId(), e.getMessage());
        }
    }
}
