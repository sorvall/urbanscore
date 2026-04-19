package com.ecorating.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Системный промпт для DeepSeek: по умолчанию из {@code classpath:prompts/deepseek-system-prompt.txt}.
 * Полная подмена — переменная окружения {@code DEEPSEEK_SYSTEM_PROMPT} (непустая строка).
 */
@Component
public class DeepSeekSystemPromptProvider {

    private final String text;

    public DeepSeekSystemPromptProvider(
            DeepSeekProperties properties, Environment environment, ResourceLoader resourceLoader) throws IOException {
        String override = trim(environment.getProperty("DEEPSEEK_SYSTEM_PROMPT"));
        if (StringUtils.hasText(override)) {
            this.text = override;
            return;
        }
        Resource resource = resourceLoader.getResource(properties.systemPromptPath());
        if (!resource.exists()) {
            throw new IllegalStateException("Файл промпта не найден: " + properties.systemPromptPath());
        }
        String loaded = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (!StringUtils.hasText(loaded)) {
            throw new IllegalStateException("Файл промпта пуст: " + properties.systemPromptPath());
        }
        this.text = loaded;
    }

    private static String trim(String raw) {
        return raw == null ? "" : raw.trim();
    }

    public String text() {
        return text;
    }
}
