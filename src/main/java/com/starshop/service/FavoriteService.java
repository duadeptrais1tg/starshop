package com.starshop.service;

import com.starshop.dto.product.ProductCardDto;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.Set;

/**
 * Sản phẩm yêu thích của user. Lượt yêu thích (Product.favoriteCount) dùng cho khối "Yêu thích nhiều nhất" ở trang chủ.
 */
public interface FavoriteService {

    int PAGE_SIZE = 12;

    /**
     * Bật / tắt yêu thích.
     *
     * @throws com.starshop.exception.NotFoundException sản phẩm không có hoặc khách không xem được (khi bật)
     */
    ToggleResult toggle(Long userId, Long productId);

    boolean isFavorite(Long userId, Long productId);

    /** Trong các sản phẩm cho trước, những sản phẩm user đã thích (để tô đỏ nút tim). */
    Set<Long> likedIds(Long userId, Collection<Long> productIds);

    /** @param page bắt đầu từ 0 */
    Page<ProductCardDto> favoritesOf(Long userId, int page);

    /**
     * @param favorited     trạng thái sau khi bấm
     * @param favoriteCount tổng lượt yêu thích hiện tại của sản phẩm
     */
    record ToggleResult(boolean favorited, int favoriteCount) {
    }
}
