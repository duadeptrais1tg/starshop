package com.starshop.dto.carrier;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Form thêm / sửa nhà vận chuyển.
 */
@Getter
@Setter
@NoArgsConstructor
public class CarrierForm {

    @NotBlank(message = "Vui lòng nhập tên nhà vận chuyển")
    @Size(max = 100, message = "Tên tối đa 100 ký tự")
    private String name;

    @NotNull(message = "Vui lòng nhập phí vận chuyển")
    @DecimalMin(value = "0", message = "Phí vận chuyển không được âm")
    @DecimalMax(value = "10000000", message = "Phí vận chuyển tối đa 10.000.000₫")
    @Digits(integer = 8, fraction = 0, message = "Phí vận chuyển là số nguyên (đồng)")
    private BigDecimal shippingFee;

    private boolean active = true;
}
