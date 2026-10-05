package com.starshop.dto.product;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Form thêm / sửa sản phẩm của vendor. Ảnh gửi riêng dạng file.
 */
@Getter
@Setter
@NoArgsConstructor
public class VendorProductForm {

    public static final int MAX_STOCK = 100_000;

    @NotBlank(message = "Vui lòng nhập tên sản phẩm")
    @Size(min = 3, max = 200, message = "Tên sản phẩm từ 3 đến 200 ký tự")
    private String name;

    @NotNull(message = "Vui lòng chọn danh mục")
    private Long categoryId;

    @NotNull(message = "Vui lòng nhập giá bán")
    @DecimalMin(value = "0", inclusive = false, message = "Giá bán phải lớn hơn 0")
    @DecimalMax(value = "1000000000", message = "Giá bán tối đa 1.000.000.000₫")
    private BigDecimal price;

    /** Giá gốc (gạch ngang), không bắt buộc; nếu có phải lớn hơn giá bán. */
    @DecimalMax(value = "1000000000", message = "Giá gốc tối đa 1.000.000.000₫")
    private BigDecimal originalPrice;

    @NotNull(message = "Vui lòng nhập số lượng tồn kho")
    @Min(value = 0, message = "Tồn kho không được âm")
    @Max(value = MAX_STOCK, message = "Tồn kho tối đa 100.000")
    private Integer stock;

    @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
    private String description;

    /** Đang bán (true) / ngừng bán (false). */
    private boolean active = true;
}
