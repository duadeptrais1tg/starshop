package com.starshop.service.impl;

import com.starshop.entity.Product;
import com.starshop.entity.User;
import com.starshop.entity.ViewedProduct;
import com.starshop.repository.ProductImageRepository;
import com.starshop.repository.ProductRepository;
import com.starshop.repository.UserRepository;
import com.starshop.repository.ViewedProductRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ViewedProductServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 3, 10, 0);

    private final ViewedProductRepository viewedRepository = mock(ViewedProductRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final ViewedProductServiceImpl service = new ViewedProductServiceImpl(
            viewedRepository, mock(ProductImageRepository.class), userRepository, productRepository,
            Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));

    @Test
    void firstView_insertsRecord() {
        when(viewedRepository.findByUserIdAndProductId(5L, 9L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(5L)).thenReturn(new User());
        when(productRepository.getReferenceById(9L)).thenReturn(new Product());

        service.record(5L, 9L);

        ArgumentCaptor<ViewedProduct> saved = ArgumentCaptor.forClass(ViewedProduct.class);
        verify(viewedRepository).save(saved.capture());
        assertThat(saved.getValue().getViewedAt()).isEqualTo(NOW);
    }

    @Test
    void viewingAgain_onlyUpdatesTime_noDuplicate() {
        ViewedProduct existing = ViewedProduct.builder().viewedAt(NOW.minusDays(3)).build();
        when(viewedRepository.findByUserIdAndProductId(5L, 9L)).thenReturn(Optional.of(existing));

        service.record(5L, 9L);

        assertThat(existing.getViewedAt()).isEqualTo(NOW);
        verify(viewedRepository, never()).save(any());
    }

    @Test
    void clear_deletesOnlyThatUsersHistory() {
        service.clear(5L);
        verify(viewedRepository).deleteByUserId(5L);
    }
}
