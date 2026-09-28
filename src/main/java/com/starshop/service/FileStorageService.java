package com.starshop.service;

import com.starshop.dto.UploadResult;
import com.starshop.entity.enums.MediaType;
import org.springframework.web.multipart.MultipartFile;

/**
 * Lưu trữ ảnh/video trên Cloudinary. Dùng chung cho ảnh sản phẩm, logo/banner shop,
 * avatar, ảnh/video đánh giá, ảnh yêu cầu trả hàng.
 */
public interface FileStorageService {

    /**
     * Upload ảnh jpg/jpeg, png, webp, tối đa 5MB.
     *
     * @param folder thư mục con, chỉ gồm a-z, 0-9, "-" và "/", ví dụ "products", "reviews/images"
     * @throws com.starshop.exception.InvalidFileException file không hợp lệ
     * @throws com.starshop.exception.FileStorageException lỗi khi lưu lên Cloudinary
     */
    UploadResult uploadImage(MultipartFile file, String folder);

    /**
     * Upload video mp4, tối đa 30MB.
     *
     * @see #uploadImage(MultipartFile, String)
     */
    UploadResult uploadVideo(MultipartFile file, String folder);

    /**
     * Xóa file trên Cloudinary. publicId rỗng thì bỏ qua; file không còn tồn tại cũng không báo lỗi.
     */
    void delete(String publicId, MediaType mediaType);
}
