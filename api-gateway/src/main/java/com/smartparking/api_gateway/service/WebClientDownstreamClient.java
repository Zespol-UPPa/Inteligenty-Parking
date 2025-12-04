package com.smartparking.api_gateway.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class WebClientDownstreamClient implements DownstreamClient{
    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
            .build();
    @Override
    public ResponseEntity<byte[]> exchange(
            HttpMethod method,
            String targetUrl,
            byte[] body,
            HttpHeaders headers) {
        var spec = webClient.method(method)
                .uri(targetUrl)
                .headers(h -> {
                h.addAll(headers);
                h.remove(HttpHeaders.HOST);
                h.remove(HttpHeaders.CONTENT_LENGTH);
            });
        if(body != null && body.length > 0){
            spec.contentType(MediaType.APPLICATION_JSON);
            return spec
                    .bodyValue(body)
                    .exchangeToMono(response -> {
                        return response.bodyToMono(byte[].class)
                                .map(bytes -> ResponseEntity.status(response.statusCode())
                                        .headers(response.headers().asHttpHeaders())
                                        .body(bytes));
                    })
                    .block();
        } else {
            return spec
                    .exchangeToMono(response -> {
                        return response.bodyToMono(byte[].class)
                                .map(bytes -> ResponseEntity.status(response.statusCode())
                                        .headers(response.headers().asHttpHeaders())
                                        .body(bytes));
                    })
                    .block();
        }

    }
}
