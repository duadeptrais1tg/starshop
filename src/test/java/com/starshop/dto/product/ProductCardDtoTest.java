package com.starshop.dto.product;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductCardDtoTest {

    @Test
    void originalPriceHigher_showsStrikeThroughAndPercent() {
        ProductCardDto card = card("550000", "650000", null, "0");

        assertThat(card.getFinalPrice()).isEqualByComparingTo("550000");
        assertThat(card.getCompareAtPrice()).isEqualByComparingTo("650000");
        assertThat(card.getDiscountPercent()).isEqualTo(15);
    }

    @Test
    void noOriginalPrice_orNotHigher_meansNoDiscount() {
        assertThat(card("550000", null, null, "0").getDiscountPercent()).isZero();
        assertThat(card("550000", "550000", null, "0").getCompareAtPrice()).isNull();
    }

    @Test
    void promotionSalePrice_takesPriority() {
        ProductCardDto card = card("500000", "650000", "400000", "0");

        assertThat(card.getFinalPrice()).isEqualByComparingTo("400000");
        assertThat(card.getCompareAtPrice()).isEqualByComparingTo("500000");
        assertThat(card.getDiscountPercent()).isEqualTo(20);
    }

    @Test
    void rating_isRoundedToHalfStar() {
        assertThat(card("1", null, null, "4.30").getRatingRounded()).isEqualTo(4.5);
        assertThat(card("1", null, null, "4.20").getRatingRounded()).isEqualTo(4.0);
        assertThat(card("1", null, null, "0").getRatingRounded()).isZero();
    }

    private static ProductCardDto card(String price, String original, String sale, String rating) {
        return ProductCardDto.builder()
                .price(new BigDecimal(price))
                .originalPrice(original == null ? null : new BigDecimal(original))
                .salePrice(sale == null ? null : new BigDecimal(sale))
                .ratingAvg(new BigDecimal(rating))
                .build();
    }
}
