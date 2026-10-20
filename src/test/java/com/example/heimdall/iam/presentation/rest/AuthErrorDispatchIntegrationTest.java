package com.example.heimdall.iam.presentation.rest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Un servidor real ejecuta el despacho ERROR del contenedor; MockMvc no lo reproduce.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthErrorDispatchIntegrationTest {
    @Value("${local.server.port}")
    private int port;

    @Test
    void unsupportedContentTypeKeepsItsStatusInsteadOfBecomingUnauthorized() throws Exception {
        var request = HttpRequest.newBuilder(uri("/api/v1/auth/register"))
                .header("Content-Type", "application/son")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"daniel\",\"password\":\"data\"}"))
                .build();
        try (var client = HttpClient.newHttpClient()) {
            assertEquals(415, client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
        }
    }

    @Test
    void invalidJsonFieldsReturnBadRequest() throws Exception {
        var request = HttpRequest.newBuilder(uri("/api/v1/auth/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"email\":\"daniel\",\"password\":\"data\"}"))
                .build();
        try (var client = HttpClient.newHttpClient()) {
            assertEquals(400, client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode());
        }
    }

    @Test
    void directRequestsToProtectedRoutesStillRequireAuthentication() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            for (String path : new String[]{"/api/v1/auth/me", "/error"}) {
                var request = HttpRequest.newBuilder(uri(path)).GET().build();
                assertEquals(401, client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode(), path);
            }
        }
    }

    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + port + path);
    }
}
