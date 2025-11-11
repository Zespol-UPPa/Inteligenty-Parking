package com.smartparking.api_gateway.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

public interface DownstreamClient {
    ResponseEntity<byte[]> exchange(
            HttpMethod method,
            String targetUrl,
            byte[] body,
            HttpHeaders headers);

}
