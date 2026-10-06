package com.starshop.dto.order;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Thống kê của shipper: tổng quan và tỉ lệ giao thành công theo tháng.
 */
@Getter
@Builder
public class ShipperStats {

    /** Tổng số lần được phân công. */
    private final long assigned;
    /** Chờ bắt đầu giao. */
    private final long waiting;
    private final long delivering;
    private final long delivered;
    private final long failed;
    private final BigDecimal codCollectedThisMonth;
    private final List<Month> months;

    /** Tỉ lệ thành công toàn thời gian (%), null nếu chưa có đơn kết thúc. */
    public Integer getSuccessRate() {
        return rate(delivered, failed);
    }

    static Integer rate(long delivered, long failed) {
        long finished = delivered + failed;
        return finished == 0 ? null : (int) Math.round(delivered * 100.0 / finished);
    }

    @Getter
    @Builder
    public static class Month {
        /** "10/2026" */
        private final String label;
        private final long assigned;
        private final long delivered;
        private final long failed;

        public Integer getSuccessRate() {
            return rate(delivered, failed);
        }
    }
}
