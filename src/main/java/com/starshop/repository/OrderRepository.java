package com.starshop.repository;

import com.starshop.entity.Order;
import com.starshop.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByCode(String code);

    boolean existsByCode(String code);

    /** Các đơn tạo trong một lần thanh toán của user (trang đặt hàng thành công). */
    @EntityGraph(attributePaths = {"shop", "items"})
    List<Order> findByPaymentTxnRefAndUserIdOrderByIdAsc(String txnRef, Long userId);

    /** Các đơn của một lần thanh toán, kèm sản phẩm (để trả lại giỏ khi thanh toán thất bại). */
    @EntityGraph(attributePaths = {"items", "items.product"})
    List<Order> findByPaymentIdOrderByIdAsc(Long paymentId);

    boolean existsByCarrierId(Long carrierId);

    /** Tổng tiền các đơn ở một trạng thái (tính trong DB, không lặp trong Java). */
    @Query("select coalesce(sum(o.total), 0) from Order o where o.status = :status")
    BigDecimal sumTotalByStatus(@Param("status") OrderStatus status);
}
