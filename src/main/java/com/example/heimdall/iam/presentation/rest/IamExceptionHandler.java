package com.example.heimdall.iam.presentation.rest;

import com.example.heimdall.iam.domain.exception.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class IamExceptionHandler {
    @ExceptionHandler(UserAlreadyExistsException.class)
    ResponseEntity<ProblemDetail> duplicate(UserAlreadyExistsException ex) {
        return problem(HttpStatus.CONFLICT, "Ya existe un usuario con ese correo");
    }
    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<ProblemDetail> credentials(InvalidCredentialsException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }
    @ExceptionHandler(UserNotFoundException.class)
    ResponseEntity<ProblemDetail> missing(UserNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Usuario no encontrado");
    }
    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ProblemDetail> invalid(Exception ex) {
        // No incluir valores rechazados: pueden contener la contraseña o detalles internos.
        return problem(HttpStatus.BAD_REQUEST, "Solicitud inválida; revisa el correo y la contraseña");
    }
    private ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
