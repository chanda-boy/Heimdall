package com.example.heimdall.iam.infrastructure.security.jwt;

import com.example.heimdall.iam.domain.exception.InvalidTokenException;
import com.example.heimdall.iam.domain.port.out.TokenServicePort;
import com.example.heimdall.iam.infrastructure.security.config.SecurityErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final TokenServicePort tokenService;

    public JwtAuthenticationFilter(TokenServicePort tokenService) {
        this.tokenService = tokenService;
    }

    /** Construye el principal desde un JWT verificado sin consultar persistencia. */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            try {
                var claims = tokenService.validateToken(authorization.substring(7).strip());
                var authentication = new UsernamePasswordAuthenticationToken(claims, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + claims.role().name())));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(authentication);
                SecurityContextHolder.setContext(context);
            } catch (InvalidTokenException exception) {
                SecurityContextHolder.clearContext();
                SecurityErrorResponse.unauthorized(response);
                return;
            }
        }
        // Las excepciones del controlador no deben reinterpretarse como fallos del token.
        filterChain.doFilter(request, response);
    }
}
