package com.starshop.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form thêm / sửa danh mục. Ảnh gửi riêng qua MultipartFile.
 */
@Getter
@Setter
@NoArgsConstructor
public class CategoryForm {

    @NotBlank(message = "Vui lòng nhập tên danh mục")
    @Size(max = 100, message = "Tên danh mục tối đa 100 ký tự")
    private String name;

    /** Để trống thì tự sinh từ tên. */
    @Size(max = 120, message = "Slug tối đa 120 ký tự")
    @Pattern(regexp = "^$|^[a-z0-9]+(-[a-z0-9]+)*$",
            message = "Slug chỉ gồm chữ thường không dấu, số và dấu gạch ngang (ví dụ hoa-sinh-nhat)")
    private String slug;

    /** Danh mục cha (tùy chọn). */
    private Long parentId;

    private boolean active = true;

    /** Tick để xóa ảnh hiện tại (khi sửa). */
    private boolean removeImage;

    /** Ảnh hiện tại, chỉ để hiển thị lại trên form sửa. */
    private String currentImageUrl;
}
