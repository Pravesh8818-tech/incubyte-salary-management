package com.incubyte.salarymanagement.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class WriteApiBearerTokenFilter extends OncePerRequestFilter {
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String UNAUTHORIZED_BODY =
            "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"A valid bearer token is required\"}";

    private final byte[] configuredToken;

    public WriteApiBearerTokenFilter(@Value("${app.security.write-api-token:}") String configuredToken) {
        this.configuredToken = configuredToken.isBlank()
                ? new byte[0]
                : configuredToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean employeePath = path.equals("/api/employees") || path.startsWith("/api/employees/");
        if (!employeePath) {
            return true;
        }

        String method = request.getMethod();
        return !("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!hasValidBearerToken(authorization)) {
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json");
            response.getWriter().write(UNAUTHORIZED_BODY);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
                "employee-write-api",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE_WRITE")));
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    private boolean hasValidBearerToken(String authorization) {
        if (configuredToken.length == 0 || authorization == null
                || !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return false;
        }

        String suppliedToken = authorization.substring(BEARER_PREFIX.length());
        if (suppliedToken.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(configuredToken, suppliedToken.getBytes(StandardCharsets.UTF_8));
    }
}
