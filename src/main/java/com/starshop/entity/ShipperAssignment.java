package com.starshop.entity;

import com.starshop.entity.enums.AssignmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Một lần phân công đơn cho shipper. Giao thất bại có thể phân công lại (tạo dòng mới).
 */
@Entity
@Table(name = "shipper_assignments")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ShipperAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipper_id", nullable = false)
    private User shipper;

    /** Vendor/Manager đã phân công. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    /** Đơn COD: shipper đã thu tiền chưa. */
    @Builder.Default
    @Column(name = "cod_collected", nullable = false)
    private boolean codCollected = false;

    @Column(name = "collected_at")
    private LocalDateTime collectedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}
