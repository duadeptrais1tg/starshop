package com.starshop.exception;

public class EmailAlreadyExistsException extends BusinessException {

    public EmailAlreadyExistsException() {
        super("Email này đã được sử dụng");
    }
}
