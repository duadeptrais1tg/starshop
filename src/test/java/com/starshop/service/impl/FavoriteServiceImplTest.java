package com.starshop.service.impl;

import com.starshop.dto.promotion.AutoPricing;
import com.starshop.entity.Category;
import com.starshop.entity.Favorite;
import com.starshop.entity.Product;
import com.starshop.entity.Shop;
import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.NotFoundException;
import com.starshop.repository.FavoriteRepository;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.FavoriteService.ToggleResult;
import com.starshop.service.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FavoriteServiceImplTest {

    private static final Long USER_ID = 7L;

    private final FavoriteRepository favoriteRepository = mock(FavoriteRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final PromotionService promotionService = mock(PromotionService.class);
    private final FavoriteServiceImpl service = new FavoriteServiceImpl(favoriteRepository, productRepository,
            mock(ProductImageRepository.class), mock(UserRepository.class), promotionService);

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(10L).name("Hoa hồng").slug("hoa-hong").price(new BigDecimal("200000"))
                .shop(Shop.builder().id(1L).name("Shop").status(ShopStatus.APPROVED).build())
                .category(Category.builder().id(2L).active(true).build())
                .active(true).build();
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(favoriteRepository.findByUserIdAndProductId(USER_ID, 10L)).thenReturn(Optional.empty());
        when(productRepository.findFavoriteCount(10L)).thenReturn(5);
        when(promotionService.autoPricing()).thenReturn(AutoPricing.none());
    }

    @Test
    void toggleOn_savesFavorite_andIncrementsCount() {
        ToggleResult result = service.toggle(USER_ID, 10L);

        assertThat(result.favorited()).isTrue();
        assertThat(result.favoriteCount()).isEqualTo(5);
        verify(favoriteRepository).save(any(Favorite.class));
        verify(productRepository).addFavoriteCount(10L, 1);
    }

    @Test
    void toggleOff_deletes_andDecrements_evenIfProductNoLongerSold() {
        Favorite existing = Favorite.builder().id(3L).product(product).build();
        when(favoriteRepository.findByUserIdAndProductId(USER_ID, 10L)).thenReturn(Optional.of(existing));
        product.setActive(false);

        ToggleResult result = service.toggle(USER_ID, 10L);

        assertThat(result.favorited()).isFalse();
        verify(favoriteRepository).delete(existing);
        verify(productRepository).addFavoriteCount(10L, -1);
        verify(favoriteRepository, never()).save(any());
    }

    @Test
    void cannotLike_hiddenOrMissingProduct() {
        product.getShop().setStatus(ShopStatus.SUSPENDED);
        assertThatThrownBy(() -> service.toggle(USER_ID, 10L)).isInstanceOf(NotFoundException.class);

        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        when(favoriteRepository.findByUserIdAndProductId(USER_ID, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.toggle(USER_ID, 99L)).isInstanceOf(NotFoundException.class);

        verify(favoriteRepository, never()).save(any());
        verify(productRepository, never()).addFavoriteCount(anyLong(), anyInt());
    }

    @Test
    void likedIds_andGuest() {
        when(favoriteRepository.findLikedProductIds(eq(USER_ID), any())).thenReturn(List.of(10L));

        assertThat(service.likedIds(USER_ID, List.of(10L, 11L))).containsExactly(10L);
        assertThat(service.likedIds(null, List.of(10L))).isEmpty();
        assertThat(service.likedIds(USER_ID, List.of())).isEmpty();
        assertThat(service.isFavorite(null, 10L)).isFalse();
    }

    @Test
    void favoritesPage_mapsToCards() {
        when(favoriteRepository.findVisibleByUserId(eq(USER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(Favorite.builder().product(product).build())));

        var page = service.favoritesOf(USER_ID, 0);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getSlug()).isEqualTo("hoa-hong");
    }
}
