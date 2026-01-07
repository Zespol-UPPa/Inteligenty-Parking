package com.smartparking.api_gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class WebClientDownstreamClient implements DownstreamClient{
    private static final Logger log = LoggerFactory.getLogger(WebClientDownstreamClient.class);
    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
            .build();
    @Override
    public ResponseEntity<byte[]> exchange(
            HttpMethod method,
            String targetUrl,
            byte[] body,
            HttpHeaders headers) {
        log.info("Calling {} {} with body size: {}", method, targetUrl, body != null ? body.length : 0);
        try {
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
                                    .defaultIfEmpty(new byte[0])
                                    .onErrorResume(WebClientException.class, e -> {
                                        log.error("Error reading response body for {} {}: {}", method, targetUrl, e.getMessage());
                                        return Mono.just(new byte[0]);
                                    })
                                    .map(bytes -> {
                                        HttpHeaders responseHeaders = new HttpHeaders(response.headers().asHttpHeaders());
                                        byte[] bodyBytes = bytes != null ? bytes : new byte[0];
                                        // Set Content-Type if not present and body is not empty
                                        if (bodyBytes.length > 0 && !responseHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
                                            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
                                        }
                                        // Remove Transfer-Encoding to avoid conflict with Content-Length
                                        responseHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
                                        // Set Content-Length to avoid chunked encoding issues
                                        if (bodyBytes.length > 0) {
                                            responseHeaders.setContentLength(bodyBytes.length);
                                        }
                                        log.info("Response from {} {}: status={}, bodySize={}", 
                                                method, targetUrl, response.statusCode(), bodyBytes.length);
                                        log.debug("Response contentType: {}, contentLength: {}", 
                                                responseHeaders.getContentType(), responseHeaders.getContentLength());
                                        return ResponseEntity.status(response.statusCode())
                                                .headers(responseHeaders)
                                                .body(bodyBytes);
                                    });
                        })
                        .block();
            } else {
                return spec
                        .exchangeToMono(response -> {
                            return response.bodyToMono(byte[].class)
                                    .defaultIfEmpty(new byte[0])
                                    .onErrorResume(WebClientException.class, e -> {
                                        log.error("Error reading response body for {} {}: {}", method, targetUrl, e.getMessage());
                                        return Mono.just(new byte[0]);
                                    })
                                    .map(bytes -> {
                                        HttpHeaders responseHeaders = new HttpHeaders(response.headers().asHttpHeaders());
                                        byte[] bodyBytes = bytes != null ? bytes : new byte[0];
                                        // Set Content-Type if not present and body is not empty
                                        if (bodyBytes.length > 0 && !responseHeaders.containsKey(HttpHeaders.CONTENT_TYPE)) {
                                            responseHeaders.setContentType(MediaType.APPLICATION_JSON);
                                        }
                                        // Remove Transfer-Encoding to avoid conflict with Content-Length
                                        responseHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
                                        // Set Content-Length to avoid chunked encoding issues
                                        if (bodyBytes.length > 0) {
                                            responseHeaders.setContentLength(bodyBytes.length);
                                        }
                                        log.info("Response from {} {}: status={}, bodySize={}", 
                                                method, targetUrl, response.statusCode(), bodyBytes.length);
                                        log.debug("Response contentType: {}, contentLength: {}", 
                                                responseHeaders.getContentType(), responseHeaders.getContentLength());
                                        return ResponseEntity.status(response.statusCode())
                                                .headers(responseHeaders)
                                                .body(bodyBytes);
                                    });
                        })
                        .block();
            }
        } catch (WebClientResponseException e) {
            log.error("WebClientResponseException for {} {}: status={}, message={}", method, targetUrl, e.getStatusCode(), e.getMessage());
            // Return response with the same status code from downstream service
            byte[] errorBody = e.getResponseBodyAsByteArray();
            if (errorBody == null || errorBody.length == 0) {
                errorBody = e.getMessage().getBytes();
            }
            HttpHeaders errorHeaders = new HttpHeaders(e.getHeaders());
            if (!errorHeaders.containsKey(HttpHeaders.CONTENT_TYPE) && errorBody.length > 0) {
                errorHeaders.setContentType(MediaType.APPLICATION_JSON);
            }
            // Remove Transfer-Encoding to avoid conflict with Content-Length
            errorHeaders.remove(HttpHeaders.TRANSFER_ENCODING);
            // Set Content-Length to avoid chunked encoding issues
            if (errorBody.length > 0) {
                errorHeaders.setContentLength(errorBody.length);
            }
            return ResponseEntity.status(e.getStatusCode())
                    .headers(errorHeaders)
                    .body(errorBody);
        } catch (Exception e) {
            log.error("Exception while calling {} {}: {}", method, targetUrl, e.getMessage(), e);
            // Return 500 Internal Server Error for unexpected errors
            String errorMessage = "Bad Gateway: " + e.getMessage();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(errorMessage.getBytes());
        }
    }
}
