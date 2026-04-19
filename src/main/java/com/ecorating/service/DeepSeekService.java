package com.ecorating.service;

import com.ecorating.config.DeepSeekProperties;
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
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class DeepSeekService {

    private final WebClient deepSeekWebClient;
    private final DeepSeekProperties properties;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public DeepSeekService(
            @Qualifier("deepSeekWebClient") WebClient deepSeekWebClient,
            DeepSeekProperties properties,
            ObjectMapper objectMapper,
            Environment environment
    ) {
        this.deepSeekWebClient = deepSeekWebClient;
        this.properties = properties;
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
        body.put(
                "messages",
                List.of(
                        Map.of("role", "system", "content", properties.systemPrompt()),
                        Map.of("role", "user", "content", userMessage)));

        String raw = deepSeekWebClient
                .post()
                .uri("/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block(properties.responseTimeout().plusSeconds(5));

        return extractAssistantHtml(raw);
    }

    private String extractAssistantHtml(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalStateException("Пустой ответ DeepSeek");
        }
        try {
            JsonNode root = objectMapper.readTree(rawJson);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                throw new IllegalStateException("Нет текста ответа в JSON DeepSeek");
            }
            String text = content.asText("").trim();
            return stripMarkdownFences(text);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Не удалось разобрать ответ DeepSeek: " + e.getMessage(), e);
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
