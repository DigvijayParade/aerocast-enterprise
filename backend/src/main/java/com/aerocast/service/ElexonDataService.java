package com.aerocast.service;

import com.aerocast.client.ElexonApiClient;
import com.aerocast.dto.ActualRecord;
import com.aerocast.dto.ForecastRecord;
import com.aerocast.dto.VintageGroup;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ElexonDataService {

    private static final Logger log = LoggerFactory.getLogger(ElexonDataService.class);

    private final ElexonApiClient apiClient;
    private final ObjectMapper objectMapper;

    // Fast in-memory cache
    private final Map<String, List<ActualRecord>> actualsMemoryCache = new ConcurrentHashMap<>();
    private final Map<String, List<ForecastRecord>> forecastsMemoryCache = new ConcurrentHashMap<>();

    public ElexonDataService(ElexonApiClient apiClient, ObjectMapper objectMapper) {
        this.apiClient = apiClient;
        this.objectMapper = objectMapper;
    }

    public List<ActualRecord> getActuals(String scope) {
        String normalizedScope = normalizeScope(scope);
        if (actualsMemoryCache.containsKey(normalizedScope) && !"live".equals(normalizedScope)) {
            return actualsMemoryCache.get(normalizedScope);
        }

        try {
            List<ActualRecord> actuals;
            if ("live".equals(normalizedScope)) {
                actuals = fetchLiveActuals();
            } else {
                actuals = fetchJan2026Actuals();
            }
            actualsMemoryCache.put(normalizedScope, actuals);
            return actuals;
        } catch (Exception e) {
            log.warn("Remote actuals fetch failed: {}. Falling back to disk/classpath cache.", e.getMessage());
            return loadFallbackActuals(normalizedScope);
        }
    }

    public List<ForecastRecord> getLatestForecasts(String scope) {
        List<ForecastRecord> allForecasts = getAllForecasts(scope);
        Map<String, ForecastRecord> latestMap = new HashMap<>();

        for (ForecastRecord fcst : allForecasts) {
            String startTime = fcst.startTime();
            if (!latestMap.containsKey(startTime) || 
                fcst.publishTime().compareTo(latestMap.get(startTime).publishTime()) > 0) {
                latestMap.put(startTime, fcst);
            }
        }

        List<ForecastRecord> result = new ArrayList<>(latestMap.values());
        result.sort(Comparator.comparing(ForecastRecord::startTime));
        return result;
    }

    public List<ForecastRecord> getForecastsByVintage(String publishBefore, String scope) {
        List<ForecastRecord> allForecasts = getAllForecasts(scope);
        Map<String, ForecastRecord> bestMap = new HashMap<>();

        for (ForecastRecord fcst : allForecasts) {
            if (fcst.publishTime() != null && fcst.publishTime().compareTo(publishBefore) <= 0) {
                String startTime = fcst.startTime();
                if (!bestMap.containsKey(startTime) || 
                    fcst.publishTime().compareTo(bestMap.get(startTime).publishTime()) > 0) {
                    bestMap.put(startTime, fcst);
                }
            }
        }

        List<ForecastRecord> result = new ArrayList<>(bestMap.values());
        result.sort(Comparator.comparing(ForecastRecord::startTime));
        return result;
    }

    public List<VintageGroup> getForecastVintages(String scope) {
        List<ForecastRecord> allForecasts = getAllForecasts(scope);
        Map<String, Set<String>> grouped = new TreeMap<>();

        for (ForecastRecord fcst : allForecasts) {
            if (fcst.startTime() != null && fcst.startTime().length() >= 10 && fcst.publishTime() != null && !fcst.publishTime().isBlank()) {
                String date = fcst.startTime().substring(0, 10);
                grouped.computeIfAbsent(date, k -> new TreeSet<>()).add(fcst.publishTime());
            }
        }

        List<VintageGroup> result = new ArrayList<>();
        grouped.forEach((date, times) -> result.add(new VintageGroup(date, new ArrayList<>(times))));
        return result;
    }

    public List<ForecastRecord> getAllForecasts(String scope) {
        String normalizedScope = normalizeScope(scope);
        if (forecastsMemoryCache.containsKey(normalizedScope) && !"live".equals(normalizedScope)) {
            return forecastsMemoryCache.get(normalizedScope);
        }

        try {
            List<ForecastRecord> forecasts;
            if ("live".equals(normalizedScope)) {
                forecasts = fetchLiveForecasts();
            } else {
                forecasts = fetchJan2026Forecasts();
            }
            forecastsMemoryCache.put(normalizedScope, forecasts);
            return forecasts;
        } catch (Exception e) {
            log.warn("Remote forecasts fetch failed: {}. Falling back to disk/classpath cache.", e.getMessage());
            return loadFallbackForecasts(normalizedScope);
        }
    }

    private List<ActualRecord> fetchJan2026Actuals() {
        String uri = "/FUELHH/stream?settlementDateFrom=2026-01-01&settlementDateTo=2026-01-31";
        List<JsonNode> nodes = apiClient.fetchDatasetStream(uri);
        return parseActuals(nodes, true);
    }

    private List<ActualRecord> fetchLiveActuals() {
        String uri = "/FUELHH/stream";
        List<JsonNode> nodes = apiClient.fetchDatasetStream(uri);
        return parseActuals(nodes, false);
    }

    private List<ActualRecord> parseActuals(List<JsonNode> nodes, boolean filterJan2026) {
        List<ActualRecord> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (JsonNode node : nodes) {
            String fuelType = getTextField(node, "fuelType", "fuel_type");
            if (!"WIND".equalsIgnoreCase(fuelType)) {
                continue;
            }

            String startTime = getTextField(node, "startTime", "start_time", "settlementDate");
            double generation = getDoubleField(node, "generation", "quantity");

            if (startTime != null && !startTime.isBlank()) {
                if (filterJan2026) {
                    String datePrefix = startTime.length() >= 10 ? startTime.substring(0, 10) : "";
                    if (datePrefix.compareTo("2026-01-01") < 0 || datePrefix.compareTo("2026-01-31") > 0) {
                        continue;
                    }
                }

                if (seen.add(startTime)) {
                    result.add(new ActualRecord(startTime, generation));
                }
            }
        }

        result.sort(Comparator.comparing(ActualRecord::startTime));
        return result;
    }

    private List<ForecastRecord> fetchJan2026Forecasts() {
        String uri = "/WINDFOR/stream?publishDateTimeFrom=2025-12-31T00:00:00Z&publishDateTimeTo=2026-02-01T00:00:00Z";
        List<JsonNode> nodes = apiClient.fetchDatasetStream(uri);
        return parseForecasts(nodes, true);
    }

    private List<ForecastRecord> fetchLiveForecasts() {
        String uri = "/WINDFOR/stream";
        List<JsonNode> nodes = apiClient.fetchDatasetStream(uri);
        return parseForecasts(nodes, false);
    }

    private List<ForecastRecord> parseForecasts(List<JsonNode> nodes, boolean filterJan2026) {
        List<ForecastRecord> result = new ArrayList<>();

        for (JsonNode node : nodes) {
            String startTime = getTextField(node, "startTime", "start_time");
            String publishTime = getTextField(node, "publishTime", "publish_time", "createdTime");
            double generation = getDoubleField(node, "generation", "quantity", "windForecast");

            if (startTime != null && !startTime.isBlank()) {
                if (filterJan2026) {
                    String datePrefix = startTime.length() >= 10 ? startTime.substring(0, 10) : "";
                    if (datePrefix.compareTo("2026-01-01") < 0 || datePrefix.compareTo("2026-01-31") > 0) {
                        continue;
                    }
                }
                result.add(new ForecastRecord(startTime, publishTime != null ? publishTime : "", generation));
            }
        }

        result.sort(Comparator.comparing(ForecastRecord::startTime).thenComparing(ForecastRecord::publishTime));
        return result;
    }

    private List<ActualRecord> loadFallbackActuals(String scope) {
        String filename = "live".equals(scope) ? "cache/actuals_live.json" : "cache/actuals_jan2026.json";
        try {
            ClassPathResource res = new ClassPathResource(filename);
            if (res.exists()) {
                try (InputStream is = res.getInputStream()) {
                    return objectMapper.readValue(is, new TypeReference<List<ActualRecord>>() {});
                }
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    private List<ForecastRecord> loadFallbackForecasts(String scope) {
        String filename = "live".equals(scope) ? "cache/forecasts_live.json" : "cache/forecasts_jan2026.json";
        try {
            ClassPathResource res = new ClassPathResource(filename);
            if (res.exists()) {
                try (InputStream is = res.getInputStream()) {
                    return objectMapper.readValue(is, new TypeReference<List<ForecastRecord>>() {});
                }
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    private String getTextField(JsonNode node, String... fieldNames) {
        for (String name : fieldNames) {
            if (node.has(name) && !node.get(name).isNull()) {
                return node.get(name).asText();
            }
        }
        return null;
    }

    private double getDoubleField(JsonNode node, String... fieldNames) {
        for (String name : fieldNames) {
            if (node.has(name) && !node.get(name).isNull()) {
                return node.get(name).asDouble();
            }
        }
        return 0.0;
    }

    private String normalizeScope(String scope) {
        return (scope != null && scope.equalsIgnoreCase("live")) ? "live" : "jan2026";
    }
}
