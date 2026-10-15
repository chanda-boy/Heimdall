package com.example.heimdall.project.presentation.rest;

import com.example.heimdall.project.domain.exception.ContactNotFoundException;
import com.example.heimdall.project.domain.exception.DuplicateProjectCodeException;
import com.example.heimdall.project.domain.exception.InvalidContactChannelException;
import com.example.heimdall.project.domain.exception.InvalidCoordinatesException;
import com.example.heimdall.project.domain.exception.ProjectAccessDeniedException;
import com.example.heimdall.project.domain.exception.ProjectNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ProjectExceptionHandler {

    @ExceptionHandler({ProjectNotFoundException.class, ContactNotFoundException.class})
    ResponseEntity<ProblemDetail> notFound(RuntimeException ex) {
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ProjectAccessDeniedException.class)
    ResponseEntity<ProblemDetail> accessDenied(ProjectAccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(DuplicateProjectCodeException.class)
    ResponseEntity<ProblemDetail> duplicateCode(DuplicateProjectCodeException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({InvalidCoordinatesException.class, InvalidContactChannelException.class})
    ResponseEntity<ProblemDetail> invalidInput(RuntimeException ex) {
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ProblemDetail> illegalState(IllegalStateException ex) {
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
