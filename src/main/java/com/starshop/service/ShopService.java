package com.starshop.service;

import com.starshop.dto.shop.MyShopDto;
import com.starshop.dto.shop.ShopForm;
import com.starshop.dto.shop.ShopPageDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * Shop của người bán: đăng ký mở shop (chờ admin duyệt), sửa thông tin trang chủ shop,
 * và trang shop công khai cho khách.
 */
public interface ShopService {

    /** Shop của user, null nếu chưa đăng ký. */
    MyShopDto findMyShop(Long userId);

    /** Form điền sẵn: thông tin shop hiện có, hoặc SĐT của user khi đăng ký lần đầu. */
    ShopForm getForm(Long userId);

    /**
     * Gửi yêu cầu mở shop (trạng thái PENDING). Shop bị từ chối được sửa và gửi lại.
     * Logo, banner không bắt buộc (null / rỗng = giữ nguyên).
     *
     * @throws com.starshop.exception.BusinessException đã có shop đang chờ duyệt / đang hoạt động, hoặc trùng tên
     * @throws com.starshop.exception.InvalidFileException ảnh không hợp lệ
     */
    void register(Long userId, ShopForm form, MultipartFile logo, MultipartFile banner);

    /**
     * Vendor sửa thông tin shop đã được duyệt. Đổi tên vẫn giữ nguyên đường dẫn /shop/{slug}.
     *
     * @throws com.starshop.exception.NotFoundException user chưa có shop đã duyệt
     */
    void update(Long ownerId, ShopForm form, MultipartFile logo, MultipartFile banner);

    /** @throws com.starshop.exception.NotFoundException không có hoặc shop chưa / không còn hoạt động */
    ShopPageDto getPublicPage(String slug);
}
