package com.ecorating.service;

import com.ecorating.config.DeepSeekProperties;
import com.ecorating.config.DeepSeekSystemPromptProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /** Сообщение для пользователя при любой ошибке ответа DeepSeek (HTTP, формат, пустой текст). */
    public static final String USER_ERROR_MESSAGE = "Извините, произошёл сбой. Повторите попытку.";

    private final OkHttpClient deepSeekHttpClient;
    private final DeepSeekProperties properties;
    private final DeepSeekSystemPromptProvider systemPromptProvider;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    public DeepSeekService(
            @Qualifier("deepSeekHttpClient") OkHttpClient deepSeekHttpClient,
            DeepSeekProperties properties,
            DeepSeekSystemPromptProvider systemPromptProvider,
            ObjectMapper objectMapper,
            Environment environment
    ) {
        this.deepSeekHttpClient = deepSeekHttpClient;
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

    /** URL chat/completions: {@code baseUrl} + {@code /chat/completions} (как в примерах DeepSeek). */
    public String chatCompletionsUrl() {
        String b = properties.baseUrl().trim();
        if (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b + "/chat/completions";
    }

    private Map<String, Object> chatRequestBody(String userMessage) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("search_enable", properties.enableSearch());
        body.put(
                "messages",
                List.of(
                        Map.of("role", "system", "content", systemPromptProvider.text()),
                        Map.of("role", "user", "content", userMessage)));
        return body;
    }

    /**
     * Текст для отладки UI: метод и URL (без секрета) + тело запроса в JSON (с системным и пользовательским текстом).
     */
    public String formatChatRequestForDebug(String userMessage) {
        try {
            String pretty = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(chatRequestBody(userMessage));
            return "POST " + chatCompletionsUrl() + "\n\n" + pretty;
        } catch (Exception e) {
            log.warn("DeepSeek: отладочная сериализация", e);
            return "POST " + chatCompletionsUrl() + "\n\n(ошибка сериализации)";
        }
    }

    public String complete(String userMessage) {
        String apiKey = resolveApiKey();
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "Не задан DEEPSEEK_API_KEY: задайте переменную окружения или deepseek.apiKey в конфиге и перезапустите приложение.");
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(chatRequestBody(userMessage));
        } catch (Exception e) {
            log.warn("DeepSeek: сериализация тела запроса", e);
            throw new IllegalStateException(USER_ERROR_MESSAGE);
        }

        Request request = new Request.Builder()
                .url(chatCompletionsUrl())
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(json, JSON))
                .build();

        try (Response response = deepSeekHttpClient.newCall(request).execute()) {
            String raw = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                log.warn("DeepSeek HTTP {}: {}", response.code(), raw);
                throw new IllegalStateException(USER_ERROR_MESSAGE);
            }
            return extractAssistantHtml(raw);
        } catch (IOException e) {
            log.warn(
                    "DeepSeek сеть/таймаут (лимит ответа ~{}): {}",
                    properties.responseTimeout(),
                    e.getMessage());
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
