package com.example.heimdall.iam.domain.exception;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Invalid or expired access token");
    }

    public InvalidTokenException(Throwable cause) {
        super("Invalid or expired access token", cause);
    }
}
