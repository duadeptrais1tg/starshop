package com.starshop.service;

import com.starshop.dto.commission.CommissionForm;
import com.starshop.dto.commission.CommissionRateDto;
import com.starshop.dto.commission.ShopCommissionRow;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Chiết khấu app thu của shop (%). Lưu theo từng giai đoạn hiệu lực, không ghi đè lịch sử.
 * Mức riêng của shop ưu tiên hơn mức mặc định toàn sàn.
 */
public interface CommissionService {

    int PAGE_SIZE = 10;

    /**
     * Tỉ lệ (%) áp dụng cho shop vào ngày date: mức riêng của shop, nếu không có thì mức mặc định,
     * nếu không có nữa thì 0. Dùng khi tạo đơn (chép vào Order.commissionRate) và tính doanh thu shop.
     */
    BigDecimal rateFor(Long shopId, LocalDate date);

    /** Mức mặc định toàn sàn đang áp dụng hôm nay (0 nếu chưa thiết lập). */
    BigDecimal currentDefaultRate();

    List<CommissionRateDto> defaultHistory();

    List<CommissionRateDto> shopHistory(Long shopId);

    /** Shop đã từng hoạt động (đang hoạt động / bị đình chỉ) kèm mức đang áp dụng. */
    Page<ShopCommissionRow> shopRows(String keyword, int page);

    /** @throws com.starshop.exception.NotFoundException shop không tồn tại */
    ShopCommissionRow shopRow(Long shopId);

    /**
     * Thêm mức mới. Mức đang chạy tự kết thúc vào ngày trước ngày bắt đầu của mức mới;
     * nếu mức mới có ngày kết thúc thì mức cũ tự tiếp tục sau đó.
     *
     * @param shopId null = mức mặc định toàn sàn
     * @throws com.starshop.exception.BusinessException ngày không hợp lệ / trùng với mức đã lên lịch
     */
    void addRate(Long shopId, CommissionForm form);

    /**
     * Xóa một mức chưa bắt đầu.
     *
     * @return shopId của mức vừa xóa (null nếu là mức mặc định) để quay lại đúng trang
     */
    Long deleteRate(Long id);
}
