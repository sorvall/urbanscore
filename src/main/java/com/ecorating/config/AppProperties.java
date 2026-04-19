package com.ecorating.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Cors cors) {
    public record Cors(String allowedOrigins) {
        public Cors {
            if (allowedOrigins == null || allowedOrigins.isBlank()) {
                allowedOrigins = "http://localhost:5173";
            }
        }
    }
}
