package com.starshop.exception;

/**
 * Lỗi phía lưu trữ (chưa cấu hình Cloudinary, mất kết nối, Cloudinary từ chối...).
 * Khác InvalidFileException: lỗi này không phải do file của người dùng.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
