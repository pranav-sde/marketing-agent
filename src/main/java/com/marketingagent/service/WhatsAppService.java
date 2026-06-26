package com.marketingagent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

@Service
public class WhatsAppService {

    @Autowired
    private ObjectMapper objectMapper;

    public boolean sendBroadcast(String accessToken, String phoneNumberId, String messageText, String mediaUrl, String recipientPhone) {
        if (accessToken == null || accessToken.trim().isEmpty() || phoneNumberId == null || phoneNumberId.trim().isEmpty()) {
            // Mock delivery when API credentials are not set
            System.out.println("[SIMULATED BROADCAST] Sending WhatsApp Message to: " + recipientPhone);
            System.out.println("Text: " + messageText);
            System.out.println("Media: " + mediaUrl);
            return true;
        }

        try {
            WebClient webClient = WebClient.builder()
                    .baseUrl("https://graph.facebook.com/v20.0")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("messaging_product", "whatsapp");
            requestBody.put("recipient_type", "individual");
            requestBody.put("to", recipientPhone);

            if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
                requestBody.put("type", "image");
                Map<String, String> imageMap = new HashMap<>();
                imageMap.put("link", mediaUrl);
                imageMap.put("caption", messageText);
                requestBody.put("image", imageMap);
            } else {
                requestBody.put("type", "text");
                Map<String, String> textMap = new HashMap<>();
                textMap.put("body", messageText);
                requestBody.put("text", textMap);
            }

            String response = webClient.post()
                    .uri("/" + phoneNumberId + "/messages")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("[WHATSAPP BROADCAST SUCCESS] Sent to " + recipientPhone + ". Response: " + response);
            return true;
        } catch (Exception e) {
            System.err.println("[WHATSAPP BROADCAST FAILED] Recipient: " + recipientPhone + ". Error: " + e.getMessage());
            return false;
        }
    }
}
