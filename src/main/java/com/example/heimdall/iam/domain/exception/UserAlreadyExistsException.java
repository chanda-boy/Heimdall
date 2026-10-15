package com.example.heimdall.iam.domain.exception;

public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException() {
        super("A user with this email already exists");
    }
}
