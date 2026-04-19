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
        /** Путь к файлу с системным промптом (classpath или file:). См. {@code DeepSeekSystemPromptProvider}. */
        String systemPromptPath,
        /** Путь к файлу с текстом пользовательского сообщения (задание отчёта). См. {@code DeepSeekUserPromptProvider}. */
        String userPromptPath,
        /** Включить веб-поиск в теле chat/completions (поле {@code search_enable}). */
        boolean enableSearch
) {
    public DeepSeekProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://api.deepseek.com/v1";
        }
        if (model == null || model.isBlank()) {
            model = "deepseek-chat";
        }
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(15);
        }
        if (responseTimeout == null) {
            responseTimeout = Duration.ofMinutes(5);
        }
        if (systemPromptPath == null || systemPromptPath.isBlank()) {
            systemPromptPath = "classpath:prompts/deepseek-system-prompt.txt";
        }
        if (userPromptPath == null || userPromptPath.isBlank()) {
            userPromptPath = "classpath:prompts/deepseek-user-prompt.txt";
        }
    }
}
