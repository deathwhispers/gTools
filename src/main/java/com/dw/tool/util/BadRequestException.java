package com.dw.tool.util;

/**
 * @author yanggj
 * @version 1.0.0
 * @date 2023/12/15 16:43
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String msg) {
        super(msg);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }
}