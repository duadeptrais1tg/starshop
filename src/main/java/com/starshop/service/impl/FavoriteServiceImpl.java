package com.starshop.service.impl;

import com.starshop.dto.product.ProductCardDto;
import com.starshop.dto.promotion.AutoPricing;
import com.starshop.entity.Favorite;
import com.starshop.entity.Product;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.NotFoundException;
import com.starshop.mapper.ProductMapper;
import com.starshop.repository.FavoriteRepository;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.FavoriteService;
import com.starshop.service.PromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final PromotionService promotionService;

    @Override
    @Transactional
    public ToggleResult toggle(Long userId, Long productId) {
        Favorite existing = favoriteRepository.findByUserIdAndProductId(userId, productId).orElse(null);
        if (existing != null) {
            // Bỏ thích luôn được (kể cả sản phẩm đã ngừng bán)
            favoriteRepository.delete(existing);
            productRepository.addFavoriteCount(productId, -1);
            return new ToggleResult(false, currentCount(productId));
        }
        Product product = productRepository.findById(productId)
                .filter(FavoriteServiceImpl::isVisible)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm"));
        favoriteRepository.save(Favorite.builder()
                .user(userRepository.getReferenceById(userId))
                .product(product)
                .build());
        productRepository.addFavoriteCount(productId, 1);
        return new ToggleResult(true, currentCount(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long productId) {
        return userId != null && favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> likedIds(Long userId, Collection<Long> productIds) {
        if (userId == null || productIds == null || productIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(favoriteRepository.findLikedProductIds(userId, productIds));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductCardDto> favoritesOf(Long userId, int page) {
        Page<Favorite> favorites = favoriteRepository.findVisibleByUserId(userId, PageRequest.of(Math.max(page, 0), PAGE_SIZE));
        List<Long> productIds = favorites.getContent().stream().map(f -> f.getProduct().getId()).toList();
        Map<Long, String> images = new HashMap<>();
        if (!productIds.isEmpty()) {
            for (Object[] row : productImageRepository.findImageUrlsByProductIds(productIds)) {
                images.putIfAbsent((Long) row[0], (String) row[1]);
            }
        }
        AutoPricing pricing = promotionService.autoPricing();
        return favorites.map(f -> {
            Product p = f.getProduct();
            return ProductMapper.toCard(p, images.get(p.getId()),
                    pricing.salePrice(p.getShop().getId(), p.getCategory().getId(), p.getPrice()));
        });
    }

    private int currentCount(Long productId) {
        Integer count = productRepository.findFavoriteCount(productId);
        return count == null ? 0 : count;
    }

    private static boolean isVisible(Product p) {
        return p.isActive() && p.getCategory().isActive() && p.getShop().getStatus() == ShopStatus.APPROVED;
    }
}
