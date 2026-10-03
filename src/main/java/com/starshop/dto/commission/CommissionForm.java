package com.starshop.dto.commission;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Form đặt mức chiết khấu mới (toàn sàn hoặc riêng một shop).
 */
@Getter
@Setter
@NoArgsConstructor
public class CommissionForm {

    @NotNull(message = "Vui lòng nhập tỉ lệ chiết khấu")
    @DecimalMin(value = "0", message = "Tỉ lệ chiết khấu từ 0 đến 100%")
    @DecimalMax(value = "100", message = "Tỉ lệ chiết khấu từ 0 đến 100%")
    @Digits(integer = 3, fraction = 2, message = "Tỉ lệ tối đa 2 chữ số thập phân")
    private BigDecimal rate;

    @NotNull(message = "Vui lòng chọn ngày bắt đầu áp dụng")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveFrom;

    /** Để trống = áp dụng đến khi có mức mới. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate effectiveTo;
}
