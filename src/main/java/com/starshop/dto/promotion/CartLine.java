package com.starshop.dto.promotion;

import java.math.BigDecimal;

/**
 * Một dòng hàng của đơn (theo shop) dùng để kiểm tra / tính mã giảm giá.
 *
 * @param categoryId danh mục của sản phẩm (để xét khuyến mãi theo danh mục)
 * @param lineTotal  thành tiền dòng = đơn giá x số lượng
 */
public record CartLine(Long categoryId, BigDecimal lineTotal) {
}
