package com.ecorating.config;

import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeepSeekClientConfig {

    /**
     * HTTP-клиент для DeepSeek API (вызовы через OkHttp, тело с {@code search_enable} и т.д.).
     */
    @Bean
    public OkHttpClient deepSeekHttpClient(DeepSeekProperties properties) {
        long connectMs = properties.connectTimeout().toMillis();
        long responseMs = properties.responseTimeout().toMillis();
        long callMs = properties.responseTimeout().plusSeconds(15).toMillis();
        return new OkHttpClient.Builder()
                .connectTimeout(connectMs, TimeUnit.MILLISECONDS)
                .readTimeout(responseMs, TimeUnit.MILLISECONDS)
                .writeTimeout(responseMs, TimeUnit.MILLISECONDS)
                .callTimeout(callMs, TimeUnit.MILLISECONDS)
                .build();
    }
}
