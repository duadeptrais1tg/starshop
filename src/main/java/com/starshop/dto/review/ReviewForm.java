package com.starshop.dto.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form đánh giá một sản phẩm trong đơn đã giao. Ảnh / video gửi riêng dạng file.
 * Độ dài nội dung (tối thiểu 50 ký tự hiển thị) kiểm tra ở service vì @Size đếm theo đơn vị UTF-16.
 */
@Getter
@Setter
@NoArgsConstructor
public class ReviewForm {

    public static final int MIN_CONTENT = 50;
    public static final int MAX_CONTENT = 2000;

    @NotNull(message = "Thiếu sản phẩm cần đánh giá")
    private Long orderItemId;

    @NotNull(message = "Vui lòng chọn số sao")
    @Min(value = 1, message = "Số sao từ 1 đến 5")
    @Max(value = 5, message = "Số sao từ 1 đến 5")
    private Integer rating;

    @NotBlank(message = "Vui lòng viết nội dung đánh giá")
    @Size(max = 4000, message = "Nội dung tối đa 2000 ký tự")
    private String content;
}
