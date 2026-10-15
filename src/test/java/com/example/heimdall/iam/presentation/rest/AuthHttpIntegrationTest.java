package com.example.heimdall.iam.presentation.rest;

import com.example.heimdall.iam.domain.model.Email;
import com.example.heimdall.iam.domain.model.Role;
import com.example.heimdall.iam.domain.model.User;
import com.example.heimdall.iam.domain.model.UserId;
import com.example.heimdall.iam.domain.port.out.PasswordEncoderPort;
import com.example.heimdall.iam.domain.port.out.TokenServicePort;
import com.example.heimdall.iam.infrastructure.persistence.repository.SpringDataUserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(AuthHttpIntegrationTest.AdminTestConfiguration.class)
class AuthHttpIntegrationTest {
    private static final String PASSWORD = "a-secure-password-123";
    @Autowired private MockMvc mvc;
    @Autowired private SpringDataUserRepository users;
    @Autowired private PasswordEncoderPort passwords;
    @Autowired private TokenServicePort tokens;

    @Test
    void registrationLoginAndMeNormalizeIdentityAndNeverExposePasswords() throws Exception {
        String email = uniqueEmail();
        MvcResult registered = mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("  " + email.toUpperCase(java.util.Locale.ROOT) + "  ", PASSWORD)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        var persisted = users.findByEmail(email).orElseThrow();
        assertEquals(Role.USER, persisted.getRole());
        assertNotEquals(PASSWORD, persisted.getPasswordHash());
        assertTrue(passwords.matches(PASSWORD, persisted.getPasswordHash()));
        assertFalse(registered.getResponse().getContentAsString().contains(persisted.getPasswordHash()));
        assertFalse(registered.getResponse().getContentAsString().contains(PASSWORD));

        MvcResult loggedIn = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email.toUpperCase(java.util.Locale.ROOT), PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.userId").value(persisted.getId().toString()))
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token(loggedIn)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.userId").value(persisted.getId().toString()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void suppliedAdminRoleCannotEscalatePublicRegistration() throws Exception {
        String email = uniqueEmail();
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"));
        assertEquals(Role.USER, users.findByEmail(email).orElseThrow().getRole());
    }

    @Test
    void duplicateNormalizedEmailReturnsConflict() throws Exception {
        String email = uniqueEmail();
        register(email);
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(" " + email.toUpperCase(java.util.Locale.ROOT) + " ", PASSWORD)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void wrongPasswordAndMissingAccountProduceIdenticalGenericResponses() throws Exception {
        String email = uniqueEmail();
        register(email);
        MvcResult wrong = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, "incorrect-password")))
                .andExpect(status().isUnauthorized()).andReturn();
        MvcResult missing = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(uniqueEmail(), "incorrect-password")))
                .andExpect(status().isUnauthorized()).andReturn();
        assertEquals(wrong.getResponse().getContentAsString(), missing.getResponse().getContentAsString());
        assertFalse(wrong.getResponse().getContentAsString().contains("incorrect-password"));
        assertFalse(wrong.getResponse().getContentAsString().contains(email));
    }

    @Test
    void invalidRegistrationAndMalformedJsonReturnBadRequestWithoutLeakingSecrets() throws Exception {
        for (String body : new String[]{credentials("invalid-email", PASSWORD), credentials(uniqueEmail(), "short"),
                credentials(uniqueEmail(), "á".repeat(37)), credentials(uniqueEmail(), ""), "{malformed"}) {
            MvcResult invalid = mvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest()).andReturn();
            assertFalse(invalid.getResponse().getContentAsString().contains(PASSWORD));
        }
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpointRejectsMissingMalformedAndUnsupportedCredentials() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"));
        for (String authorization : new String[]{"Bearer malformed", "Bearer ", "Basic abc123", "Bearer a.b.c"}) {
            mvc.perform(get("/api/v1/auth/me").header("Authorization", authorization))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void authenticationDoesNotCreateSessionOrCarryOverToNextRequest() throws Exception {
        MvcResult registered = register(uniqueEmail());
        assertNull(registered.getRequest().getSession(false));
        assertNull(registered.getResponse().getCookie("JSESSIONID"));
        MvcResult authorized = mvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token(registered)))
                .andExpect(status().isOk()).andReturn();
        assertNull(authorized.getRequest().getSession(false));
        assertNull(authorized.getResponse().getCookie("JSESSIONID"));
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void methodSecurityDistinguishesMissingIdentityInsufficientRoleAndAdmin() throws Exception {
        mvc.perform(get("/test/iam/admin")).andExpect(status().isUnauthorized());
        mvc.perform(get("/test/iam/admin").header("Authorization", "Bearer " + tokenFor(Role.USER)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/test/iam/admin").header("Authorization", "Bearer " + tokenFor(Role.ADMIN)))
                .andExpect(status().isOk()).andExpect(content().string("admin"));
    }

    private MvcResult register(String email) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, PASSWORD)))
                .andExpect(status().isCreated()).andReturn();
    }

    private String token(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private String tokenFor(Role role) {
        return tokens.generateToken(new User(UserId.random(), new Email(uniqueEmail()), "fixture-hash", role));
    }

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    private String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AdminTestConfiguration {
        @Bean
        AdminEndpoint adminEndpoint() {
            return new AdminEndpoint();
        }
    }

    @TestComponent
    @RestController
    public static class AdminEndpoint {
        @GetMapping("/test/iam/admin")
        @PreAuthorize("hasRole('ADMIN')")
        public String admin() {
            return "admin";
        }
    }
}
