package com.ecorating.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cors cors,
        /**
         * Временно: отдавать в {@code POST /api/v1/report} текст тела запроса к DeepSeek (без API-ключа) для отладки UI.
         * В проде задайте {@code APP_DEBUG_DEEPSEEK_REQUEST=false}.
         */
        boolean debugExposeDeepseekRequest
) {
    public record Cors(String allowedOrigins) {
        public Cors {
            if (allowedOrigins == null || allowedOrigins.isBlank()) {
                allowedOrigins = "*";
            }
        }
    }

    public AppProperties {
        if (cors == null) {
            cors = new Cors(null);
        }
    }
}
