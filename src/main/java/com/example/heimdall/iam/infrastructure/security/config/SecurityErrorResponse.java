package com.example.heimdall.iam.infrastructure.security.config;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class SecurityErrorResponse {
    private SecurityErrorResponse() { }

    public static void unauthorized(HttpServletResponse response) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        write(response, 401, "Unauthorized", "Authentication is required or the access token is invalid");
    }

    public static void forbidden(HttpServletResponse response) throws IOException {
        write(response, 403, "Forbidden", "You do not have permission to access this resource");
    }

    private static void write(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"status\":" + status + ",\"error\":\"" + error
                + "\",\"message\":\"" + message + "\"}");
    }
}
