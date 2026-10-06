package com.starshop.repository;

import com.starshop.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
