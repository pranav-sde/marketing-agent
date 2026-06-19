package com.marketingagent.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Holds the configured API key used for authenticating requests.
 * The key is read from the MARKETING_AGENT_API_KEY environment variable
 * or defaults to a development placeholder.
 */
@Component
public class ApiKeyProperties {

    private final String apiKey;

    public ApiKeyProperties(@Value("${marketing-agent.security.api-key:}") String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
}
