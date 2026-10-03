package com.starshop.dto.review;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Tổng hợp đánh giá của một sản phẩm: điểm trung bình, tổng số, số lượng theo từng mức sao.
 */
@Getter
@AllArgsConstructor
public class RatingSummary {

    private final BigDecimal average;
    private final long total;
    /** Khóa 5..1 -> số đánh giá. */
    private final Map<Integer, Long> countByStar;

    /** Số đánh giá ở một mức sao (gọi từ JSP: EL tự đổi số sang int, tránh lỗi khóa Long/Integer của Map). */
    public long count(int star) {
        return countByStar.getOrDefault(star, 0L);
    }

    /** % đánh giá ở một mức sao (vẽ thanh tiến độ). */
    public int percent(int star) {
        if (total == 0) {
            return 0;
        }
        return (int) Math.round(countByStar.getOrDefault(star, 0L) * 100.0 / total);
    }
}
