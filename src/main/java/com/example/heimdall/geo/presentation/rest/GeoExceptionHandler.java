package com.example.heimdall.geo.presentation.rest;

import com.example.heimdall.geo.domain.exception.DepartmentNotFoundException;
import com.example.heimdall.geo.domain.exception.InvalidGeoCodeException;
import com.example.heimdall.geo.domain.exception.MunicipalityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GeoExceptionHandler {

    @ExceptionHandler({DepartmentNotFoundException.class, MunicipalityNotFoundException.class})
    ResponseEntity<ProblemDetail> notFound(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(InvalidGeoCodeException.class)
    ResponseEntity<ProblemDetail> invalidCode(InvalidGeoCodeException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
