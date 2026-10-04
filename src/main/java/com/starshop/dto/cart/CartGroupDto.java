package com.starshop.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Các dòng giỏ hàng của cùng một shop (mỗi shop sẽ thành một đơn khi thanh toán).
 */
@Getter
@AllArgsConstructor
public class CartGroupDto {

    private final Long shopId;
    private final String shopName;
    private final String shopSlug;
    private final List<CartLineDto> lines;

    public boolean isHasAvailable() {
        return lines.stream().anyMatch(CartLineDto::isAvailable);
    }
}
