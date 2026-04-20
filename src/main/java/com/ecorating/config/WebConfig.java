package com.ecorating.config;

import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties appProperties;

    public WebConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String raw = appProperties.cors() != null && appProperties.cors().allowedOrigins() != null
                ? appProperties.cors().allowedOrigins()
                : "*";
        String[] patterns =
                Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toArray(String[]::new);
        // allowedOriginPatterns: * — любой Origin (нужно для доступа и по домену, и по IP:порту)
        registry.addMapping("/api/**").allowedOriginPatterns(patterns).allowedMethods("*").allowedHeaders("*");
    }
}
