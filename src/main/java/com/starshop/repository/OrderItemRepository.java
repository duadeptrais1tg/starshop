package com.starshop.repository;

import com.starshop.dto.revenue.RevenueProjections;
import com.starshop.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /** Sản phẩm đã có trong đơn hàng nào chưa (đã có đơn thì không xóa, chỉ ẩn). */
    boolean existsByProductId(Long productId);

    /** Tổng số sản phẩm của nhiều đơn trong 1 query. Mỗi phần tử: [orderId (Long), tổng số lượng (Long)]. */
    @Query("select i.order.id, sum(i.quantity) from OrderItem i where i.order.id in :orderIds group by i.order.id")
    List<Object[]> sumQuantityByOrderIds(@Param("orderIds") Collection<Long> orderIds);

    /** Sản phẩm của nhiều đơn trong 1 query (danh sách đơn của khách). */
    List<OrderItem> findByOrderIdInOrderByIdAsc(Collection<Long> orderIds);

    /** Top sản phẩm bán chạy của shop (đơn đã giao trong khoảng thời gian), theo số lượng bán. */
    @Query("select p.id as productId, p.name as name, p.slug as slug,"
            + " sum(i.quantity) as quantity, sum(i.lineTotal) as revenue"
            + " from OrderItem i join i.order o join i.product p"
            + " where o.shop.id = :shopId and o.status = com.starshop.entity.enums.OrderStatus.DELIVERED"
            + " and o.deliveredAt >= :from and o.deliveredAt < :to"
            + " group by p.id, p.name, p.slug order by sum(i.quantity) desc, sum(i.lineTotal) desc")
    List<RevenueProjections.TopProduct> topProducts(@Param("shopId") Long shopId, @Param("from") LocalDateTime from,
                                                    @Param("to") LocalDateTime to, Pageable pageable);
}
