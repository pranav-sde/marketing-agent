package com.marketingagent.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;

@Component
public class ApiKeyFilter implements Filter {

    @Value("${marketing-agent.security.api-key:Press@001}")
    private String configuredApiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        // Bypass checks for H2 console, Actuator, Uploads, and CORS OPTIONS preflights
        if ("OPTIONS".equalsIgnoreCase(method)
                || path.startsWith("/h2-console")
                || path.startsWith("/actuator")
                || path.startsWith("/v1/uploads")) {
            chain.doFilter(request, response);
            return;
        }

        // Enforce X-API-KEY validation for standard API endpoints under /v1/
        if (path.startsWith("/v1/")) {
            String apiKeyHeader = httpRequest.getHeader("X-API-KEY");
            if (configuredApiKey.equals(apiKeyHeader)) {
                chain.doFilter(request, response);
            } else {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"Invalid or missing X-API-KEY header.\"}");
            }
        } else {
            chain.doFilter(request, response);
        }
    }
}
