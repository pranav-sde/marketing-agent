package com.marketingagent.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

/**
 * Filter that validates the X-API-KEY header on incoming requests.
 * If the key matches, the request is authenticated as an API client.
 * If no API key is configured in the application properties, the filter
 * logs a warning and allows all requests through (development mode).
 */
@Component
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);
    private static final String API_KEY_HEADER = "X-API-KEY";

    private final ApiKeyProperties apiKeyProperties;

    public ApiKeyAuthenticationFilter(ApiKeyProperties apiKeyProperties) {
        this.apiKeyProperties = apiKeyProperties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // If no API key is configured, allow all requests (dev mode)
        if (!apiKeyProperties.isConfigured()) {
            LOGGER.warn("No API key configured — all requests are permitted. Set marketing-agent.security.api-key in production.");
            setAuthenticated("anonymous-dev");
            filterChain.doFilter(request, response);
            return;
        }

        String providedKey = request.getHeader(API_KEY_HEADER);

        if (providedKey != null && providedKey.equals(apiKeyProperties.getApiKey())) {
            setAuthenticated("api-client");
            filterChain.doFilter(request, response);
        } else {
            LOGGER.debug("Rejected request to {} — invalid or missing API key", request.getRequestURI());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"Invalid or missing API key\"}");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Allow webhook endpoints, actuator health checks, and static uploads without API key
        return path.startsWith("/webhooks/") || path.startsWith("/actuator/") || path.startsWith("/uploads/");
    }

    private void setAuthenticated(String principal) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_API_CLIENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
