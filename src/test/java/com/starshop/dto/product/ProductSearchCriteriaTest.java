package com.starshop.dto.product;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSearchCriteriaTest {

    @Test
    void defaults_toNewestFirstPage_andNotFiltered() {
        ProductSearchCriteria c = new ProductSearchCriteria().normalize();

        assertThat(c.getSortOption()).isEqualTo(ProductSort.NEWEST);
        assertThat(c.getSort()).isEqualTo("NEWEST");
        assertThat(c.getPage()).isEqualTo(1);
        assertThat(c.isFiltered()).isFalse();
    }

    @Test
    void invalidValuesFromUrl_areIgnoredOrFixed() {
        ProductSearchCriteria c = new ProductSearchCriteria();
        c.setKeyword("   ");
        c.setMinPrice(new BigDecimal("-5"));
        c.setRating(9);
        c.setSort("bat-ky");
        c.setPage(-3);

        c.normalize();

        assertThat(c.getKeyword()).isNull();
        assertThat(c.getMinPrice()).isNull();
        assertThat(c.getRating()).isNull();
        assertThat(c.getSortOption()).isEqualTo(ProductSort.NEWEST);
        assertThat(c.getPage()).isEqualTo(1);
    }

    @Test
    void swappedPriceRange_isSwappedBack() {
        ProductSearchCriteria c = new ProductSearchCriteria();
        c.setMinPrice(new BigDecimal("900000"));
        c.setMaxPrice(new BigDecimal("300000"));

        c.normalize();

        assertThat(c.getMinPrice()).isEqualByComparingTo("300000");
        assertThat(c.getMaxPrice()).isEqualByComparingTo("900000");
        assertThat(c.isFiltered()).isTrue();
    }

    @Test
    void sortIsCaseInsensitive_andKeywordTrimmedAndCapped() {
        ProductSearchCriteria c = new ProductSearchCriteria();
        c.setSort("price_desc");
        c.setKeyword("  " + "a".repeat(150) + "  ");

        c.normalize();

        assertThat(c.getSortOption()).isEqualTo(ProductSort.PRICE_DESC);
        assertThat(c.getKeyword()).hasSize(ProductSearchCriteria.MAX_KEYWORD_LENGTH);
    }
}
