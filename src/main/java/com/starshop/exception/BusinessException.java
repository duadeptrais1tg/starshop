package com.starshop.exception;

/**
 * Lỗi nghiệp vụ có thể báo thẳng cho người dùng (message tiếng Việt).
 * Các lỗi nghiệp vụ cụ thể kế thừa class này.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
