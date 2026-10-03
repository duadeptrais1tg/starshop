package com.starshop.dto.promotion;

import com.starshop.entity.enums.PromotionScope;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AutoPricingTest {

    @Test
    void picksBestDiscount_andRespectsScopeAndCap() {
        AutoPricing pricing = new AutoPricing(List.of(
                new AutoPricing.Rule(PromotionScope.PLATFORM, null, null, new BigDecimal("10"), null),
                new AutoPricing.Rule(PromotionScope.CATEGORY, null, 1L, new BigDecimal("30"), new BigDecimal("100000")),
                new AutoPricing.Rule(PromotionScope.SHOP, 7L, null, new BigDecimal("20"), null)),
                Map.of(2L, 1L));   // danh mục 2 là con của danh mục 1

        // Toàn sàn 10%: 500k -> 450k
        assertThat(pricing.salePrice(9L, 5L, new BigDecimal("500000"))).isEqualByComparingTo("450000");
        // Danh mục con của 1: 30% của 500k = 150k nhưng tối đa 100k -> 400k
        assertThat(pricing.salePrice(9L, 2L, new BigDecimal("500000"))).isEqualByComparingTo("400000");
        // Shop 7: 20% -> 400k (tốt hơn 10% toàn sàn)
        assertThat(pricing.salePrice(7L, 5L, new BigDecimal("500000"))).isEqualByComparingTo("400000");
    }

    @Test
    void noRules_meansNoSalePrice() {
        assertThat(AutoPricing.none().salePrice(1L, 1L, new BigDecimal("100000"))).isNull();
    }
}
