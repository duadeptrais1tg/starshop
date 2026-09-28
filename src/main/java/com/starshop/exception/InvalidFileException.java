package com.starshop.exception;

/**
 * File người dùng gửi lên không hợp lệ (rỗng, sai định dạng, quá dung lượng...).
 * Message viết bằng tiếng Việt để hiển thị thẳng cho người dùng.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
