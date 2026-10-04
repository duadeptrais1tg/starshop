package com.starshop.dto.checkout;

import com.starshop.dto.account.AddressDto;
import com.starshop.dto.carrier.CarrierDto;
import com.starshop.entity.enums.PaymentMethod;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Toàn bộ dữ liệu trang checkout: lựa chọn hiện tại, các đơn theo shop, tổng tiền và lỗi chặn đặt hàng.
 */
@Getter
@Builder
public class CheckoutView {

    private final List<Long> itemIds;
    private final List<AddressDto> addresses;
    private final Long addressId;
    private final List<CarrierDto> carriers;
    private final Long carrierId;
    private final List<PaymentMethod> paymentMethods;
    private final PaymentMethod paymentMethod;
    private final List<CheckoutGroup> groups;
    /** Lỗi chặn đặt hàng (chưa có địa chỉ, sản phẩm hết hàng, mã không hợp lệ...). */
    private final List<String> errors;

    public boolean isCanPlace() {
        return errors.isEmpty();
    }

    /** "1,2,3" để đưa lại vào URL / form. */
    public String getItemIdsParam() {
        return itemIds.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    public int getItemCount() {
        return groups.stream().mapToInt(g -> g.getLines().stream().mapToInt(CheckoutLine::getQuantity).sum()).sum();
    }

    public BigDecimal getSubtotal() {
        return sum(CheckoutGroup::getSubtotal);
    }

    public BigDecimal getProductDiscount() {
        return sum(CheckoutGroup::getProductDiscount);
    }

    public BigDecimal getShippingFee() {
        return sum(CheckoutGroup::getShippingFee);
    }

    public BigDecimal getShippingDiscount() {
        return sum(CheckoutGroup::getShippingDiscount);
    }

    public BigDecimal getTotal() {
        return sum(CheckoutGroup::getTotal);
    }

    private BigDecimal sum(java.util.function.Function<CheckoutGroup, BigDecimal> field) {
        return groups.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
