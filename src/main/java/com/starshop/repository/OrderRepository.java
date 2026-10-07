package com.starshop.repository;

import com.starshop.dto.revenue.RevenueProjections;
import com.starshop.entity.Order;
import com.starshop.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByCode(String code);

    boolean existsByCode(String code);

    /** Danh sách có phân trang (đơn của vendor / của khách), nạp sẵn thanh toán + nhà vận chuyển + shop. */
    @Override
    @EntityGraph(attributePaths = {"payment", "carrier", "shop"})
    Page<Order> findAll(Specification<Order> spec, Pageable pageable);

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

    // ======================================================== Thống kê doanh thu shop (chỉ đơn DELIVERED, theo ngày giao)

    @Query("select count(o) as orderCount,"
            + " coalesce(sum(o.subtotal - o.productDiscount), 0) as revenue,"
            + " coalesce(sum((o.subtotal - o.productDiscount) * o.commissionRate / 100), 0) as commission"
            + " from Order o where o.shop.id = :shopId and o.status = com.starshop.entity.enums.OrderStatus.DELIVERED"
            + " and o.deliveredAt >= :from and o.deliveredAt < :to")
    RevenueProjections.Totals revenueTotals(@Param("shopId") Long shopId,
                                            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Doanh thu theo ngày giao. */
    @Query(value = "select date_format(o.delivered_at, '%Y-%m-%d') as period,"
            + " sum(o.subtotal - o.product_discount) as revenue,"
            + " sum((o.subtotal - o.product_discount) * o.commission_rate / 100) as commission, count(*) as orders"
            + " from orders o where o.shop_id = :shopId and o.status = 'DELIVERED'"
            + " and o.delivered_at >= :from and o.delivered_at < :to"
            + " group by date_format(o.delivered_at, '%Y-%m-%d') order by period", nativeQuery = true)
    List<RevenueProjections.Point> revenueByDay(@Param("shopId") Long shopId,
                                                @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Doanh thu theo tháng giao. */
    @Query(value = "select date_format(o.delivered_at, '%Y-%m') as period,"
            + " sum(o.subtotal - o.product_discount) as revenue,"
            + " sum((o.subtotal - o.product_discount) * o.commission_rate / 100) as commission, count(*) as orders"
            + " from orders o where o.shop_id = :shopId and o.status = 'DELIVERED'"
            + " and o.delivered_at >= :from and o.delivered_at < :to"
            + " group by date_format(o.delivered_at, '%Y-%m') order by period", nativeQuery = true)
    List<RevenueProjections.Point> revenueByMonth(@Param("shopId") Long shopId,
                                                  @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Số đơn theo trạng thái (đơn đặt trong khoảng thời gian), chỉ đơn shop thấy được: COD hoặc đã thanh toán online.
     * Mỗi phần tử: [OrderStatus, count].
     */
    @Query("select o.status, count(o) from Order o left join o.payment p where o.shop.id = :shopId"
            + " and o.createdAt >= :from and o.createdAt < :to"
            + " and (o.paymentMethod = com.starshop.entity.enums.PaymentMethod.COD"
            + " or p.status in (com.starshop.entity.enums.PaymentStatus.PAID, com.starshop.entity.enums.PaymentStatus.REFUNDED))"
            + " group by o.status")
    List<Object[]> countByStatusForShop(@Param("shopId") Long shopId,
                                        @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
