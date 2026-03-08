package com.danapple.spicep.coincap;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.Collections;

abstract class AbstractCoinCapService {
    private final String apiKey;
    private final String url;

    AbstractCoinCapService(String apiKey, String url) {
        this.apiKey = apiKey;
        this.url = url;
    }

    HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.add("user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.99 Safari/537.36");
        headers.add("Authorization", "Bearer %s".formatted(apiKey));
        return headers;
    }

    String getUrl() {
        return url;
    }
}
