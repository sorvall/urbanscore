package com.ecorating.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deepseek")
public record DeepSeekProperties(
        String baseUrl,
        String apiKey,
        String model,
        Duration connectTimeout,
        Duration responseTimeout,
        String systemPrompt
) {
    public DeepSeekProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.deepseek.com";
        }
        if (model == null || model.isBlank()) {
            model = "deepseek-chat";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(15);
        }
        if (responseTimeout == null) {
            responseTimeout = Duration.ofMinutes(2);
        }
        if (systemPrompt == null) {
            systemPrompt = "";
        }
    }
}
