package com.marketingagent.webclient;

import io.netty.channel.ChannelOption;
import java.util.function.Consumer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(WhatsAppClientProperties.class)
public class WebClientConfig {

    @Bean
    public WebClient whatsAppWebClient(WhatsAppClientProperties properties, WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Math.toIntExact(properties.getConnectTimeout().toMillis()))
                .responseTimeout(properties.getResponseTimeout());

        return builder
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeaders(defaultHeaders(properties))
                .build();
    }

    private Consumer<HttpHeaders> defaultHeaders(WhatsAppClientProperties properties) {
        return headers -> {
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (properties.getAccessToken() != null && !properties.getAccessToken().isBlank()) {
                headers.setBearerAuth(properties.getAccessToken());
            }
        };
    }
}
