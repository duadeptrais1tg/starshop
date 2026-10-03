package com.starshop.service.impl;

import com.starshop.dto.product.ProductCardDto;
import com.starshop.dto.promotion.AutoPricing;
import com.starshop.entity.Product;
import com.starshop.entity.User;
import com.starshop.entity.ViewedProduct;
import com.starshop.mapper.ProductMapper;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.UserRepository;
import com.starshop.repository.ViewedProductRepository;
import com.starshop.service.PromotionService;
import com.starshop.service.ViewedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ViewedProductServiceImpl implements ViewedProductService {

    private final ViewedProductRepository viewedProductRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final Clock clock;
    private final PromotionService promotionService;

    @Override
    @Transactional
    public void record(Long userId, Long productId) {
        LocalDateTime now = LocalDateTime.now(clock);
        viewedProductRepository.findByUserIdAndProductId(userId, productId)
                .ifPresentOrElse(
                        viewed -> viewed.setViewedAt(now),
                        () -> {
                            // getReferenceById: chỉ cần khóa ngoại, không query thêm user/product
                            User user = userRepository.getReferenceById(userId);
                            Product product = productRepository.getReferenceById(productId);
                            viewedProductRepository.save(ViewedProduct.builder()
                                    .user(user).product(product).viewedAt(now).build());
                        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCardDto> viewedBy(Long userId, int page) {
        Page<ViewedProduct> viewed = viewedProductRepository.findVisibleByUserId(
                userId, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
        List<Long> productIds = viewed.getContent().stream().map(v -> v.getProduct().getId()).toList();
        Map<Long, String> images = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Object[] row : productImageRepository.findImageUrlsByProductIds(productIds)) {
                images.putIfAbsent((Long) row[0], (String) row[1]);
            }
        }
        AutoPricing pricing = promotionService.autoPricing();
        return viewed.map(v -> {
            Product p = v.getProduct();
            return ProductMapper.toCard(p, images.get(p.getId()),
                    pricing.salePrice(p.getShop().getId(), p.getCategory().getId(), p.getPrice()));
        });
    }

    @Override
    @Transactional
    public void clear(Long userId) {
        viewedProductRepository.deleteByUserId(userId);
    }
}
