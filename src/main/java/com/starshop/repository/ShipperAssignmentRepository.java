package com.starshop.repository;

import com.starshop.entity.ShipperAssignment;
import com.starshop.entity.enums.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipperAssignmentRepository extends JpaRepository<ShipperAssignment, Long> {

    /** Lần phân công gần nhất của đơn (mỗi lần giao lại sẽ tạo bản ghi mới). */
    @EntityGraph(attributePaths = "shipper")
    Optional<ShipperAssignment> findFirstByOrderIdOrderByIdDesc(Long orderId);

    /** Số đơn đang giao dở của từng shipper (để vendor chọn người ít việc). [shipperId, count] */
    @Query("select a.shipper.id, count(a) from ShipperAssignment a where a.shipper.id in :shipperIds"
            + " and a.status in (com.starshop.entity.enums.AssignmentStatus.ASSIGNED,"
            + " com.starshop.entity.enums.AssignmentStatus.DELIVERING) group by a.shipper.id")
    List<Object[]> countActiveByShipperIds(@Param("shipperIds") Collection<Long> shipperIds);

    /** Đơn được giao cho shipper theo trạng thái phân công, nạp sẵn đơn + shop. */
    @EntityGraph(attributePaths = {"order", "order.shop"})
    Page<ShipperAssignment> findByShipperIdAndStatus(Long shipperId, AssignmentStatus status, Pageable pageable);

    /** Chỉ tìm trong các lần phân công của chính shipper -> không thao tác được đơn của người khác. */
    @EntityGraph(attributePaths = {"order", "order.shop", "order.carrier"})
    Optional<ShipperAssignment> findByIdAndShipperId(Long id, Long shipperId);

    /** Số lần phân công theo trạng thái của shipper. Mỗi phần tử: [AssignmentStatus, count]. */
    @Query("select a.status, count(a) from ShipperAssignment a where a.shipper.id = :shipperId group by a.status")
    List<Object[]> countByStatus(@Param("shipperId") Long shipperId);

    /** Thống kê theo tháng (từ một thời điểm). Mỗi phần tử: [năm, tháng, AssignmentStatus, count]. */
    @Query("select extract(year from a.createdAt), extract(month from a.createdAt), a.status, count(a)"
            + " from ShipperAssignment a where a.shipper.id = :shipperId and a.createdAt >= :from"
            + " group by extract(year from a.createdAt), extract(month from a.createdAt), a.status")
    List<Object[]> monthlyStats(@Param("shipperId") Long shipperId, @Param("from") LocalDateTime from);

    /** Tổng tiền COD shipper đã thu từ một thời điểm. */
    @Query("select coalesce(sum(a.order.total), 0) from ShipperAssignment a"
            + " where a.shipper.id = :shipperId and a.codCollected = true and a.collectedAt >= :from")
    BigDecimal sumCodCollectedSince(@Param("shipperId") Long shipperId, @Param("from") LocalDateTime from);
}
