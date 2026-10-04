package com.starshop.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Toàn bộ giỏ hàng, nhóm theo shop.
 */
@Getter
@AllArgsConstructor
public class CartView {

    private final List<CartGroupDto> groups;

    public int getLineCount() {
        return groups.stream().mapToInt(g -> g.getLines().size()).sum();
    }

    public boolean isEmpty() {
        return groups.isEmpty();
    }
}
