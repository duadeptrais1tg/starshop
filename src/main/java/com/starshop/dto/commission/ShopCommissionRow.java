package com.starshop.dto.commission;

import com.starshop.entity.enums.ShopStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Một shop trong danh sách chiết khấu: mức đang áp dụng hôm nay (riêng hoặc mặc định).
 */
@Getter
@Builder
public class ShopCommissionRow {

    private final Long shopId;
    private final String shopName;
    private final String ownerEmail;
    private final ShopStatus status;
    /** Mức riêng của shop đang hiệu lực (null = đang dùng mức mặc định). */
    private final BigDecimal customRate;
    /** Mức thực tế đang áp dụng. */
    private final BigDecimal effectiveRate;

    public boolean isCustom() {
        return customRate != null;
    }
}
