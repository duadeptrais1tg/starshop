package com.starshop.service;

import com.starshop.dto.order.ShipperOrderDetail;
import com.starshop.dto.order.ShipperOrderRow;
import com.starshop.dto.order.ShipperStats;
import com.starshop.entity.enums.AssignmentStatus;
import org.springframework.data.domain.Page;

import java.util.Map;

/**
 * Shipper xử lý đơn được phân công. Chỉ thao tác trên lần phân công của chính mình và còn hiệu lực
 * (đơn chưa được giao cho shipper khác); đổi trạng thái đơn qua OrderService.
 */
public interface ShipperOrderService {

    int PAGE_SIZE = 10;
    int STATS_MONTHS = 6;

    /** @param page bắt đầu từ 0 */
    Page<ShipperOrderRow> search(Long shipperId, AssignmentStatus status, int page);

    Map<AssignmentStatus, Long> countByStatus(Long shipperId);

    /** @param assignmentId id lần phân công */
    ShipperOrderDetail detail(Long shipperId, Long assignmentId);

    /** Bắt đầu giao: đơn PICKED_UP -> SHIPPING (đơn được giao lại thì đã ở SHIPPING). */
    void startDelivery(Long shipperId, Long assignmentId);

    /** Đã giao: đơn SHIPPING -> DELIVERED; đơn COD đánh dấu đã thu tiền. */
    void markDelivered(Long shipperId, Long assignmentId);

    /** Giao thất bại (bắt buộc lý do): đơn giữ SHIPPING, chờ giao lại / giao shipper khác. */
    void markFailed(Long shipperId, Long assignmentId, String reason);

    /** Giao lại đơn vừa thất bại. */
    void retry(Long shipperId, Long assignmentId);

    ShipperStats stats(Long shipperId);
}
