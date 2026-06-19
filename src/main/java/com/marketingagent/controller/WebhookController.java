package com.marketingagent.controller;

import com.marketingagent.domain.common.ProviderType;
import com.marketingagent.dto.message.WebhookIngestRequest;
import com.marketingagent.service.WebhookEventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/webhooks/whatsapp")
public class WebhookController {

    private final WebhookEventService webhookEventService;
    private final ObjectMapper objectMapper;

    public WebhookController(WebhookEventService webhookEventService, ObjectMapper objectMapper) {
        this.webhookEventService = webhookEventService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/{providerAccountId}")
    public ResponseEntity<String> verifyWebhook(
            @PathVariable UUID providerAccountId,
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String verifyToken,
            @RequestParam(value = "hub.challenge", required = false) String challenge
    ) {
        // Echo challenge back if mode is 'subscribe'
        if ("subscribe".equals(mode) && challenge != null) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @PostMapping("/{providerAccountId}")
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            @PathVariable UUID providerAccountId,
            @RequestBody Map<String, Object> payload
    ) {
        try {
            // Serialize payload to string to generate a unique hash for idempotency check
            String payloadStr = objectMapper.writeValueAsString(payload);
            String eventHash = generateSha256(payloadStr);

            // Attempt to extract provider event ID (Message ID) from standard Meta payload structure
            String providerEventId = extractMetaEventId(payload);

            WebhookIngestRequest request = new WebhookIngestRequest(
                    providerAccountId,
                    ProviderType.WHATSAPP_CLOUD_API,
                    providerEventId,
                    eventHash,
                    payload
            );

            UUID eventId = webhookEventService.ingestWebhook(request);
            return ResponseEntity.ok(Map.of("status", "success", "event_id", eventId));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    private String generateSha256(String text) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @SuppressWarnings("unchecked")
    private String extractMetaEventId(Map<String, Object> payload) {
        try {
            List<Map<String, Object>> entries = (List<Map<String, Object>>) payload.get("entry");
            if (entries != null && !entries.isEmpty()) {
                Map<String, Object> entry = entries.get(0);
                List<Map<String, Object>> changes = (List<Map<String, Object>>) entry.get("changes");
                if (changes != null && !changes.isEmpty()) {
                    Map<String, Object> change = changes.get(0);
                    Map<String, Object> value = (Map<String, Object>) change.get("value");
                    if (value != null) {
                        // Check for status update message ID
                        List<Map<String, Object>> statuses = (List<Map<String, Object>>) value.get("statuses");
                        if (statuses != null && !statuses.isEmpty()) {
                            return (String) statuses.get(0).get("id");
                        }
                        // Check for incoming message ID
                        List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");
                        if (messages != null && !messages.isEmpty()) {
                            return (String) messages.get(0).get("id");
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall back to null if payload mapping doesn't match expected Meta Cloud API format
        }
        return null;
    }
}
