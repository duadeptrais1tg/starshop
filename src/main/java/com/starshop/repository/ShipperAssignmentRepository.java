package com.starshop.repository;

import com.starshop.entity.ShipperAssignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
