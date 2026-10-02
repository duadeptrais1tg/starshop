package com.starshop.service;

import com.starshop.dto.OptionDto;
import com.starshop.dto.category.CategoryDto;
import com.starshop.dto.category.CategoryForm;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Quản lý danh mục sản phẩm. Dùng chung cho Admin và Manager (controller khác nhau, cùng nghiệp vụ).
 */
public interface CategoryService {

    int PAGE_SIZE = 10;

    /**
     * @param active null = tất cả, true = đang hiển thị, false = đang ẩn
     * @param page   bắt đầu từ 0
     */
    Page<CategoryDto> search(String keyword, Boolean active, int page);

    /** Dữ liệu điền sẵn vào form sửa. */
    CategoryForm getForm(Long id);

    /**
     * Danh mục có thể chọn làm cha: bỏ chính nó và các danh mục con/cháu của nó (tránh vòng lặp).
     *
     * @param editingId null khi tạo mới
     */
    List<OptionDto> parentOptions(Long editingId);

    /** Danh mục đang hiển thị (dùng cho form sản phẩm, bộ lọc trang khách). */
    List<OptionDto> activeOptions();

    /** @return id danh mục vừa tạo */
    Long create(CategoryForm form, MultipartFile image);

    void update(Long id, CategoryForm form, MultipartFile image);

    /** Bật / tắt hiển thị. */
    void toggleActive(Long id);

    /**
     * @throws com.starshop.exception.BusinessException danh mục đang có sản phẩm hoặc danh mục con
     */
    void delete(Long id);
}
