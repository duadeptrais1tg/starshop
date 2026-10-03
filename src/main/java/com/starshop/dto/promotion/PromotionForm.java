package com.starshop.dto.promotion;

import com.starshop.entity.enums.PromotionScope;
import com.starshop.entity.enums.PromotionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Form thêm / sửa chương trình khuyến mãi.
 * Mã coupon để trống = khuyến mãi tự áp dụng (khi đó số lượt / giới hạn mỗi người không dùng tới).
 */
@Getter
@Setter
@NoArgsConstructor
public class PromotionForm {

    @NotBlank(message = "Vui lòng nhập tên chương trình")
    @Size(max = 150, message = "Tên tối đa 150 ký tự")
    private String name;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description;

    @NotNull(message = "Vui lòng chọn loại khuyến mãi")
    private PromotionType type;

    @NotNull(message = "Vui lòng chọn phạm vi áp dụng")
    private PromotionScope scope = PromotionScope.PLATFORM;

    /** Bắt buộc khi phạm vi là danh mục. */
    private Long categoryId;

    /** % giảm (giảm giá sản phẩm) hoặc số tiền giảm (giảm phí vận chuyển). */
    @NotNull(message = "Vui lòng nhập giá trị giảm")
    @DecimalMin(value = "0.01", message = "Giá trị giảm phải lớn hơn 0")
    private BigDecimal discountValue;

    /** Số tiền giảm tối đa (chỉ cho giảm %), để trống = không giới hạn. */
    @DecimalMin(value = "0", message = "Giảm tối đa không được âm")
    private BigDecimal maxDiscount;

    @DecimalMin(value = "0", message = "Đơn tối thiểu không được âm")
    private BigDecimal minOrderValue;

    @NotNull(message = "Vui lòng chọn thời gian bắt đầu")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startAt;

    @NotNull(message = "Vui lòng chọn thời gian kết thúc")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endAt;

    @Pattern(regexp = "^$|^[A-Za-z0-9]{4,20}$", message = "Mã gồm 4–20 chữ cái hoặc chữ số, không dấu, không khoảng trắng")
    private String couponCode;

    /** Tổng số lượt dùng mã (để trống = không giới hạn). */
    @Min(value = 1, message = "Số lượt phải từ 1 trở lên")
    private Integer usageLimit;

    @NotNull(message = "Vui lòng nhập số lần mỗi người được dùng")
    @Min(value = 1, message = "Mỗi người dùng ít nhất 1 lần")
    private Integer perUserLimit = 1;

    private boolean active = true;
}
