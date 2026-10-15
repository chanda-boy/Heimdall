package com.example.heimdall.geo.domain.exception;

public class InvalidGeoCodeException extends RuntimeException {
    public InvalidGeoCodeException(String message) {
        super(message);
    }
}
