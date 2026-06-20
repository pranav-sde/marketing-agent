package com.marketingagent.webclient.whatsapp;

import com.marketingagent.exception.ExternalProviderException;
import com.marketingagent.webclient.WhatsAppClientProperties;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Validated
public class WhatsAppMessageClient {

    private final WebClient whatsAppWebClient;
    private final WhatsAppClientProperties properties;

    public WhatsAppMessageClient(
            @Qualifier("whatsAppWebClient") WebClient whatsAppWebClient,
            WhatsAppClientProperties properties
    ) {
        this.whatsAppWebClient = whatsAppWebClient;
        this.properties = properties;
    }

    public Mono<WhatsAppMessageResponse> sendTemplateMessage(
            String phoneNumberId,
            @Valid WhatsAppTemplateMessageRequest request
    ) {
        return sendTemplateMessage(properties.getAccessToken(), phoneNumberId, request);
    }

    public Mono<WhatsAppMessageResponse> sendTemplateMessage(
            String accessToken,
            String phoneNumberId,
            @Valid WhatsAppTemplateMessageRequest request
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", request.to());
        payload.put("type", "template");
        payload.put("template", Map.of(
                "name", request.templateName(),
                "language", Map.of("code", request.languageCode()),
                "components", request.components() == null ? java.util.List.of() : request.components()
        ));

        return whatsAppWebClient.post()
                .uri("/{version}/{phoneNumberId}/messages", properties.getApiVersion(), phoneNumberId)
                .headers(headers -> {
                    if (accessToken != null && !accessToken.isBlank()) {
                        headers.setBearerAuth(accessToken);
                    }
                })
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("WhatsApp provider request failed")
                                .map(ExternalProviderException::new)
                )
                .bodyToMono(WhatsAppMessageResponse.class);
    }

    public Mono<WhatsAppMessageResponse> sendTextMessage(
            String phoneNumberId,
            String to,
            String text
    ) {
        return sendTextMessage(properties.getAccessToken(), phoneNumberId, to, text);
    }

    public Mono<WhatsAppMessageResponse> sendTextMessage(
            String accessToken,
            String phoneNumberId,
            String to,
            String text
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "text");
        payload.put("text", Map.of("body", text, "preview_url", true));

        return whatsAppWebClient.post()
                .uri("/{version}/{phoneNumberId}/messages", properties.getApiVersion(), phoneNumberId)
                .headers(headers -> {
                    if (accessToken != null && !accessToken.isBlank()) {
                        headers.setBearerAuth(accessToken);
                    }
                })
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("WhatsApp provider request failed")
                                .map(ExternalProviderException::new)
                )
                .bodyToMono(WhatsAppMessageResponse.class);
    }

    public Mono<WhatsAppMessageResponse> sendImageMessage(
            String accessToken,
            String phoneNumberId,
            String to,
            String imageUrl,
            String caption
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", to);
        payload.put("type", "image");
        payload.put("image", Map.of("link", imageUrl, "caption", caption));

        return whatsAppWebClient.post()
                .uri("/{version}/{phoneNumberId}/messages", properties.getApiVersion(), phoneNumberId)
                .headers(headers -> {
                    if (accessToken != null && !accessToken.isBlank()) {
                        headers.setBearerAuth(accessToken);
                    }
                })
                .bodyValue(payload)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), response ->
                        response.bodyToMono(String.class)
                                .defaultIfEmpty("WhatsApp provider request failed")
                                .map(ExternalProviderException::new)
                )
                .bodyToMono(WhatsAppMessageResponse.class);
    }
}
