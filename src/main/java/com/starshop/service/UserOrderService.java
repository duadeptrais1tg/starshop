package com.starshop.service;

import com.starshop.dto.order.CancelReason;
import com.starshop.dto.order.UserOrderDetail;
import com.starshop.dto.order.UserOrderRow;
import com.starshop.dto.order.UserOrderTab;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * "Đơn hàng của tôi": khách xem lịch sử mua hàng, hủy đơn, yêu cầu trả hàng.
 * Chỉ thao tác trên đơn của chính mình (đơn người khác -> không tìm thấy); đổi trạng thái qua OrderService.
 */
public interface UserOrderService {

    int PAGE_SIZE = 10;
    int MAX_RETURN_IMAGES = 5;

    /** @param page bắt đầu từ 0 */
    Page<UserOrderRow> search(Long userId, UserOrderTab tab, int page);

    Map<UserOrderTab, Long> countByTab(Long userId);

    UserOrderDetail detail(Long userId, Long orderId);

    /**
     * Hủy đơn còn chờ xác nhận: OrderService hoàn tồn kho và lượt mã.
     *
     * @param otherReason nội dung khi chọn "Lý do khác"
     * @throws com.starshop.exception.BusinessException chưa chọn lý do / đơn không hủy được
     */
    void cancel(Long userId, Long orderId, CancelReason reason, String otherReason);

    /**
     * Yêu cầu trả hàng khi đơn đã giao, trong hạn N ngày, kèm lý do và 1–5 ảnh minh chứng.
     *
     * @throws com.starshop.exception.BusinessException quá hạn / đã yêu cầu / thiếu lý do hoặc ảnh
     */
    void requestReturn(Long userId, Long orderId, String reason, List<MultipartFile> images);
}
