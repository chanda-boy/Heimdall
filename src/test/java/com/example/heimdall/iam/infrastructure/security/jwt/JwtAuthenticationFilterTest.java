package com.example.heimdall.iam.infrastructure.security.jwt;

import com.example.heimdall.iam.domain.exception.InvalidTokenException;
import com.example.heimdall.iam.domain.model.Email;
import com.example.heimdall.iam.domain.model.Role;
import com.example.heimdall.iam.domain.model.TokenClaims;
import com.example.heimdall.iam.domain.model.UserId;
import com.example.heimdall.iam.domain.port.out.TokenServicePort;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final TokenServicePort tokens = mock(TokenServicePort.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokens);
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain chain = mock(FilterChain.class);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesBearerAndExposesRole() throws Exception {
        var claims = new TokenClaims(UserId.random(), new Email("user@example.com"), Role.USER);
        request.addHeader("Authorization", "bearer access-token");
        when(tokens.validateToken("access-token")).thenReturn(claims);

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(claims, authentication.getPrincipal());
        assertTrue(authentication.isAuthenticated());
        assertEquals("ROLE_USER", authentication.getAuthorities().iterator().next().getAuthority());
        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidBearerReturnsJsonAndStopsChain() throws Exception {
        request.addHeader("Authorization", "Bearer invalid");
        when(tokens.validateToken("invalid")).thenThrow(new InvalidTokenException());

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        assertTrue(response.getContentType().startsWith("application/json"));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(chain);
    }

    @Test
    void noBearerContinuesWithoutCheckingTokens() throws Exception {
        filter.doFilter(request, response, chain);

        verifyNoInteractions(tokens);
        verify(chain).doFilter(request, response);
    }

    @Test
    void downstreamExceptionIsNotReportedAsInvalidBearer() throws Exception {
        request.addHeader("Authorization", "Bearer valid");
        when(tokens.validateToken("valid")).thenReturn(new TokenClaims(UserId.random(), new Email("user@example.com"), Role.ADMIN));
        var downstreamFailure = new InvalidTokenException();
        doThrow(downstreamFailure).when(chain).doFilter(request, response);

        assertSame(downstreamFailure, assertThrows(InvalidTokenException.class,
                () -> filter.doFilter(request, response, chain)));
        assertEquals(200, response.getStatus());
    }
}
