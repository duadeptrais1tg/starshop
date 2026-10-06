package com.starshop.repository.spec;

import com.starshop.entity.Order;
import com.starshop.entity.Payment;
import com.starshop.entity.enums.OrderStatus;
import com.starshop.entity.enums.PaymentMethod;
import com.starshop.entity.enums.PaymentStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Collection;

/**
 * Điều kiện tìm đơn hàng. Điều kiện null = không lọc.
 */
public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> shop(Long shopId) {
        return (root, query, cb) -> cb.equal(root.get("shop").get("id"), shopId);
    }

    public static Specification<Order> statusIn(Collection<OrderStatus> statuses) {
        return statuses == null || statuses.isEmpty() ? null : (root, query, cb) -> root.get("status").in(statuses);
    }

    /**
     * Đơn shop cần xử lý: COD, hoặc thanh toán online đã trả tiền (kể cả đã hoàn tiền).
     * Đơn online chưa thanh toán / thanh toán thất bại không hiện cho shop.
     */
    public static Specification<Order> visibleToShop() {
        return (root, query, cb) -> {
            Join<Order, Payment> payment = root.join("payment", JoinType.LEFT);
            return cb.or(
                    cb.equal(root.get("paymentMethod"), PaymentMethod.COD),
                    payment.get("status").in(PaymentStatus.PAID, PaymentStatus.REFUNDED));
        };
    }

    /** Mã đơn chứa chuỗi (không phân biệt hoa thường). */
    public static Specification<Order> codeContains(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        String pattern = SpecUtils.likePattern(code);
        return (root, query, cb) -> cb.like(cb.lower(root.get("code")), pattern, SpecUtils.ESCAPE);
    }

    /** Ngày đặt từ ... (tính cả ngày). */
    public static Specification<Order> createdFrom(LocalDate from) {
        return from == null ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from.atStartOfDay());
    }

    /** Ngày đặt đến ... (tính cả ngày). */
    public static Specification<Order> createdTo(LocalDate to) {
        return to == null ? null
                : (root, query, cb) -> cb.lessThan(root.get("createdAt"), to.plusDays(1).atStartOfDay());
    }
}
