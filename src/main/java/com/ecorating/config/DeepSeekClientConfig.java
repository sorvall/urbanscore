package com.ecorating.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class DeepSeekClientConfig {

    @Bean
    public WebClient deepSeekWebClient(WebClient.Builder builder, DeepSeekProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(properties.responseTimeout())
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) properties.connectTimeout().toMillis());
        return builder
                .baseUrl(properties.baseUrl().endsWith("/")
                        ? properties.baseUrl().substring(0, properties.baseUrl().length() - 1)
                        : properties.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
