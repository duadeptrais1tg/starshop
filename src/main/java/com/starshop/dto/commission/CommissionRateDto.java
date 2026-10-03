package com.starshop.dto.commission;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Một giai đoạn chiết khấu trong lịch sử.
 */
@Getter
@Builder
public class CommissionRateDto {

    private final Long id;
    private final BigDecimal rate;
    private final String effectiveFrom;
    /** "" nếu không có ngày kết thúc. */
    private final String effectiveTo;
    private final Status status;

    /** Chỉ xóa được mức chưa bắt đầu (mức đã/đang áp dụng là dữ liệu lịch sử). */
    public boolean isDeletable() {
        return status == Status.UPCOMING;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Status {
        ACTIVE("Đang áp dụng"),
        UPCOMING("Sắp áp dụng"),
        EXPIRED("Đã kết thúc");

        private final String label;
    }
}
