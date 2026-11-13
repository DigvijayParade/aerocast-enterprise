package com.aerocast.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class ElexonApiClient {

    private static final Logger log = LoggerFactory.getLogger(ElexonApiClient.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public ElexonApiClient(
            @Value("${app.elexon.base-url:https://data.elexon.co.uk/bmrs/api/v1/datasets}") String baseUrl,
            @Value("${app.elexon.timeout-seconds:60}") int timeoutSeconds,
            ObjectMapper objectMapper) {
        
        this.objectMapper = objectMapper;

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(timeoutSeconds));

        // Allow up to 32MB payload in memory for full month dataset streams
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
                .build();

        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<JsonNode> fetchDatasetStream(String uri) {
        try {
            log.info("Fetching Elexon dataset stream: {}", uri);
            String rawJson = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(90));

            if (rawJson == null || rawJson.isBlank()) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(rawJson);
            List<JsonNode> result = new ArrayList<>();

            if (root.isArray()) {
                root.forEach(result::add);
            } else if (root.isObject()) {
                for (String key : List.of("data", "items", "results")) {
                    if (root.has(key) && root.get(key).isArray()) {
                        root.get(key).forEach(result::add);
                        break;
                    }
                }
            }
            log.info("Successfully received {} records from {}", result.size(), uri);
            return result;
        } catch (Exception e) {
            log.error("Failed to fetch from Elexon stream {}: {}", uri, e.getMessage());
            throw new RuntimeException("Elexon upstream error: " + e.getMessage(), e);
        }
    }
}
