package com.starshop.dto;

import com.starshop.entity.enums.MediaType;

/**
 * Kết quả upload lên Cloudinary.
 *
 * @param url       đường dẫn https để hiển thị
 * @param publicId  mã file trên Cloudinary, lưu lại để xóa sau này
 * @param mediaType ảnh hay video
 */
public record UploadResult(String url, String publicId, MediaType mediaType) {
}
