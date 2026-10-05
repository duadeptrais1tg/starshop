package com.starshop.service;

import com.starshop.dto.product.VendorProductForm;
import com.starshop.dto.product.VendorProductImage;
import com.starshop.dto.product.VendorProductRow;
import com.starshop.dto.product.VendorProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Vendor quản lý sản phẩm của shop mình. Mọi thao tác đều kiểm tra sản phẩm thuộc shop của vendor;
 * sản phẩm của shop khác được coi như không tồn tại (NotFoundException).
 */
public interface VendorProductService {

    int PAGE_SIZE = 10;
    int MAX_IMAGES = 8;

    /** @param page bắt đầu từ 0 */
    Page<VendorProductRow> search(Long ownerId, String keyword, Long categoryId, VendorProductStatus status, int page);

    /** Số sản phẩm theo từng trạng thái (cho tab lọc). */
    long countAll(Long ownerId);

    VendorProductForm getForm(Long ownerId, Long productId);

    List<VendorProductImage> images(Long ownerId, Long productId);

    /** Tên + slug để hiển thị link "Xem trên trang khách". */
    String slugOf(Long ownerId, Long productId);

    /**
     * Thêm sản phẩm, bắt buộc ít nhất 1 ảnh; ảnh đầu tiên là ảnh đại diện.
     *
     * @return id sản phẩm mới
     */
    Long create(Long ownerId, VendorProductForm form, List<MultipartFile> images);

    /** Sửa thông tin, thêm ảnh mới (nếu có). Đổi tên vẫn giữ nguyên đường dẫn sản phẩm. */
    void update(Long ownerId, Long productId, VendorProductForm form, List<MultipartFile> newImages);

    void setThumbnail(Long ownerId, Long productId, Long imageId);

    /** @throws com.starshop.exception.BusinessException xóa ảnh cuối cùng */
    void deleteImage(Long ownerId, Long productId, Long imageId);

    /** Đang bán <-> ngừng bán. */
    void toggleActive(Long ownerId, Long productId);

    /**
     * Xóa sản phẩm. Sản phẩm đã có trong đơn hàng thì chỉ ẩn (ngừng bán) để giữ lịch sử đơn.
     *
     * @return true nếu đã xóa hẳn, false nếu chỉ ẩn
     */
    boolean delete(Long ownerId, Long productId);
}
