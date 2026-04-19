package com.ecorating.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Пользовательское сообщение для DeepSeek (задание отчёта): по умолчанию из
 * {@code classpath:prompts/deepseek-user-prompt.txt}. Полная подмена — {@code DEEPSEEK_USER_PROMPT}.
 */
@Component
public class DeepSeekUserPromptProvider {

    private final String text;

    public DeepSeekUserPromptProvider(
            DeepSeekProperties properties, Environment environment, ResourceLoader resourceLoader) throws IOException {
        String override = trim(environment.getProperty("DEEPSEEK_USER_PROMPT"));
        if (StringUtils.hasText(override)) {
            this.text = override;
            return;
        }
        Resource resource = resourceLoader.getResource(properties.userPromptPath());
        if (!resource.exists()) {
            throw new IllegalStateException("Файл пользовательского промпта не найден: " + properties.userPromptPath());
        }
        String loaded = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (!StringUtils.hasText(loaded)) {
            throw new IllegalStateException("Файл пользовательского промпта пуст: " + properties.userPromptPath());
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
