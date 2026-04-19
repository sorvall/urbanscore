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
        /** Включить веб-поиск в запросе к API (поле {@code enable_search} в теле chat/completions). */
        boolean enableSearch,
        /**
         * Режим поиска (поле {@code search_mode}), например {@code smart} — «Умный поиск» в интерфейсе DeepSeek.
         * Пустая строка — не отправлять параметр (если API не поддерживает).
         */
        String searchMode
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
            responseTimeout = Duration.ofMinutes(5);
        }
        if (systemPromptPath == null || systemPromptPath.isBlank()) {
            systemPromptPath = "classpath:prompts/deepseek-system-prompt.txt";
        }
        if (userPromptPath == null || userPromptPath.isBlank()) {
            userPromptPath = "classpath:prompts/deepseek-user-prompt.txt";
        }
        if (searchMode == null) {
            searchMode = "smart";
        }
    }
}
