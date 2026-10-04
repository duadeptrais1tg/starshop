package com.starshop.service;

import com.starshop.dto.cart.CartChangeResult;
import com.starshop.dto.cart.CartView;

import java.util.Collection;

/**
 * Giỏ hàng lưu database theo user. Mọi thao tác chỉ trên giỏ của userId truyền vào
 * (lấy từ phiên đăng nhập); dòng của người khác -> NotFoundException.
 */
public interface CartService {

    /** Số lượng tối đa của một dòng (chống nhập nhầm số quá lớn). */
    int MAX_QUANTITY_PER_ITEM = 99;

    CartView getCart(Long userId);

    /** Số dòng trong giỏ (badge trên header). */
    long countLines(Long userId);

    /**
     * Thêm vào giỏ; đã có thì cộng dồn. Không vượt tồn kho.
     *
     * @throws com.starshop.exception.BusinessException sản phẩm ngừng bán / hết hàng / vượt tồn kho
     */
    CartChangeResult addItem(Long userId, Long productId, int quantity);

    /**
     * Đặt lại số lượng của một dòng (1 .. tồn kho).
     *
     * @throws com.starshop.exception.BusinessException số lượng không hợp lệ
     */
    CartChangeResult updateQuantity(Long userId, Long itemId, int quantity);

    /** Xóa các dòng của chính user (id không thuộc giỏ của user bị bỏ qua). */
    int removeItems(Long userId, Collection<Long> itemIds);
}
