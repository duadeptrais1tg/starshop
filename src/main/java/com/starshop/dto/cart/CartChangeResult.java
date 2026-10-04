package com.starshop.dto.cart;

import java.math.BigDecimal;

/**
 * Kết quả JSON sau khi thêm / sửa giỏ hàng bằng AJAX.
 *
 * @param cartCount    số dòng trong giỏ (cập nhật badge header)
 * @param quantity     số lượng hiện tại của dòng vừa thay đổi
 * @param lineTotal    thành tiền dòng vừa thay đổi
 * @param message      thông báo hiển thị cho người dùng
 */
public record CartChangeResult(long cartCount, int quantity, BigDecimal lineTotal, String message) {
}
