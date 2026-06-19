package com.marketingagent.webclient.groq;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.Map;

@Component
public class GroqClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroqClient.class);

    private final WebClient webClient;
    private final GroqClientProperties properties;
    private final ObjectMapper objectMapper;

    public GroqClient(WebClient.Builder webClientBuilder, GroqClientProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(properties.getResponseTimeout());

        this.webClient = webClientBuilder
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Generates a chat completion using the default configured model.
     * @param systemPrompt Optional system prompt.
     * @param userPrompt The user prompt to send.
     * @return The response text content.
     */
    public String generateCompletion(String systemPrompt, String userPrompt) {
        return generateCompletion(properties.getDefaultModel(), systemPrompt, userPrompt, 0.7);
    }

    /**
     * Generates a chat completion specifying the model and temperature.
     */
    public String generateCompletion(String model, String systemPrompt, String userPrompt, double temperature) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank() || properties.getApiKey().contains("placeholder")) {
            LOGGER.warn("Groq API key is not configured or is a placeholder. Returning dummy response.");
            return generateDummyResponse(userPrompt);
        }

        List<Map<String, String>> messages;
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            );
        } else {
            messages = List.of(
                    Map.of("role", "user", "content", userPrompt)
            );
        }

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", messages,
                "temperature", temperature
        );

        LOGGER.debug("Sending request to Groq API with model: {}", model);

        try {
            String responseStr = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode responseJson = objectMapper.readTree(responseStr);
            return responseJson.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            LOGGER.error("Failed to call Groq API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate content from Groq", e);
        }
    }
    
    private String generateDummyResponse(String prompt) {
        if (prompt.contains("extract stories") || prompt.contains("Extract all stories")) {
            return """
            [
              {
                "title": "The Future of AI in Marketing",
                "summary": "AI is reshaping how we personalize content at scale.",
                "keywords": ["AI", "Marketing", "Personalization"],
                "contentAngle": "Educational",
                "pageNumber": 1
              },
              {
                "title": "Top 5 Travel Destinations 2026",
                "summary": "Exploring the hidden gems for your next vacation.",
                "keywords": ["Travel", "Vacation", "2026"],
                "contentAngle": "Inspirational",
                "pageNumber": 3
              }
            ]
            """;
        }
        
        return "👋 Check out our latest update! 🚀\\n\\n" + 
               prompt.substring(0, Math.min(prompt.length(), 50)) + "...\\n\\n" +
               "Subscribe: https://campaign.sailortoday.in/campaign?utmMedium=whatsapp\\n\\n" +
               "#MarketingAgent #Update";
    }
}
