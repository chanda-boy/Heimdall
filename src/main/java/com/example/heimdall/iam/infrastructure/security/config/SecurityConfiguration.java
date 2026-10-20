package com.example.heimdall.iam.infrastructure.security.config;

import com.example.heimdall.iam.domain.port.out.TokenServicePort;
import com.example.heimdall.iam.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.example.heimdall.iam.infrastructure.security.jwt.JwtProperties;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfiguration {
    /** Usa UTC para que emisión y validación compartan la misma referencia temporal. */
    @Bean
    public Clock jwtClock() {
        return Clock.systemUTC();
    }

    /** Exige identidad en cada petición y permite únicamente registro y login sin credenciales. */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TokenServicePort tokenService)
            throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // Conserva el error HTTP original en el despacho interno del contenedor.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/geo/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> SecurityErrorResponse.unauthorized(response))
                        .accessDeniedHandler((request, response, exception) -> SecurityErrorResponse.forbidden(response)))
                .addFilterBefore(new JwtAuthenticationFilter(tokenService), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
