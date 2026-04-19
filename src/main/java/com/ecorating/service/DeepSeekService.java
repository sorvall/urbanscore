package com.ecorating.service;

import com.ecorating.config.DeepSeekProperties;
import com.ecorating.config.DeepSeekSystemPromptProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    /** Сообщение для пользователя при любой ошибке ответа DeepSeek (HTTP, формат, пустой текст). */
    public static final String USER_ERROR_MESSAGE = "Извините, произошёл сбой. Повторите попытку.";

    private final WebClient deepSeekWebClient;
    private final DeepSeekProperties properties;
    private final DeepSeekSystemPromptProvider systemPromptProvider;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public DeepSeekService(
            @Qualifier("deepSeekWebClient") WebClient deepSeekWebClient,
            DeepSeekProperties properties,
            DeepSeekSystemPromptProvider systemPromptProvider,
            ObjectMapper objectMapper,
            Environment environment
    ) {
        this.deepSeekWebClient = deepSeekWebClient;
        this.properties = properties;
        this.systemPromptProvider = systemPromptProvider;
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    /**
     * Ключ читаем из нескольких источников: иногда {@code @ConfigurationProperties} не подхватывает переменную из
     * Docker env / .env, хотя она есть в {@link System#getenv()}.
     */
    private String resolveApiKey() {
        String k = trimKey(properties.apiKey());
        if (StringUtils.hasText(k)) {
            return k;
        }
        k = trimKey(environment.getProperty("DEEPSEEK_API_KEY"));
        if (StringUtils.hasText(k)) {
            return k;
        }
        k = trimKey(System.getenv("DEEPSEEK_API_KEY"));
        if (StringUtils.hasText(k)) {
            return k;
        }
        return trimKey(environment.getProperty("deepseek.apiKey"));
    }

    private static String trimKey(String raw) {
        if (raw == null) {
            return null;
        }
        String t = raw.trim();
        if (t.startsWith("\uFEFF")) {
            t = t.substring(1).trim();
        }
        if ((t.startsWith("\"") && t.endsWith("\"")) || (t.startsWith("'") && t.endsWith("'"))) {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }

    public String complete(String userMessage) {
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "Не задан DEEPSEEK_API_KEY: задайте переменную окружения или deepseek.apiKey в конфиге и перезапустите приложение.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("enable_search", properties.enableSearch());
        if (properties.enableSearch() && StringUtils.hasText(properties.searchMode())) {
            body.put("search_mode", properties.searchMode().trim());
        }
        body.put(
                "messages",
                List.of(
                        Map.of("role", "system", "content", systemPromptProvider.text()),
                        Map.of("role", "user", "content", userMessage)));

        try {
            // Согласовано с HttpClient.responseTimeout в DeepSeekClientConfig; запас на сеть.
            var wait = properties.responseTimeout().plusSeconds(15);
            String raw = deepSeekWebClient
                    .post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(wait);

            return extractAssistantHtml(raw);
        } catch (WebClientResponseException e) {
            log.warn("DeepSeek HTTP {}: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new IllegalStateException(USER_ERROR_MESSAGE);
        } catch (WebClientRequestException e) {
            log.warn("DeepSeek сеть/таймаут: {}", e.getMessage());
            throw new IllegalStateException(USER_ERROR_MESSAGE);
        } catch (IllegalStateException e) {
            if (USER_ERROR_MESSAGE.equals(e.getMessage())) {
                throw e;
            }
            if (e.getMessage() != null && e.getMessage().contains("Timeout on blocking read")) {
                log.warn(
                        "DeepSeek: истекло время ожидания ответа (лимит ~{}). Увеличьте DEEPSEEK_RESPONSE_TIMEOUT в .env / application.yml.",
                        properties.responseTimeout());
            } else {
                log.warn("DeepSeek ответ не обработан: {}", e.getMessage());
            }
            throw new IllegalStateException(USER_ERROR_MESSAGE);
        } catch (Exception e) {
            log.warn("DeepSeek неожиданная ошибка", e);
            throw new IllegalStateException(USER_ERROR_MESSAGE);
        }
    }

    private String extractAssistantHtml(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            log.warn("DeepSeek: пустое тело ответа");
            throw new IllegalStateException(USER_ERROR_MESSAGE);
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            if (root.has("error")) {
                String apiMsg = root.path("error").path("message").asText(root.path("error").asText("error"));
                log.warn("DeepSeek error в JSON: {}", apiMsg);
                throw new IllegalStateException(USER_ERROR_MESSAGE);
            }
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                log.warn("DeepSeek: нет choices в ответе");
                throw new IllegalStateException(USER_ERROR_MESSAGE);
            }
            JsonNode content = choices.path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                log.warn("DeepSeek: нет message.content");
                throw new IllegalStateException(USER_ERROR_MESSAGE);
            }
            String text = content.asText("").trim();
            if (!StringUtils.hasText(text)) {
                log.warn("DeepSeek: пустой content");
                throw new IllegalStateException(USER_ERROR_MESSAGE);
            }
            return stripMarkdownFences(text);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.warn("DeepSeek: разбор JSON: {}", e.getMessage());
            throw new IllegalStateException(USER_ERROR_MESSAGE);
        }
    }

    private static String stripMarkdownFences(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) {
                t = t.substring(firstNl + 1);
            }
            int end = t.lastIndexOf("```");
            if (end > 0) {
                t = t.substring(0, end);
            }
        }
        return t.trim();
    }
}
