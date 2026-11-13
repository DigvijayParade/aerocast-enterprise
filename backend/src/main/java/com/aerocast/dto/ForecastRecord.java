package com.aerocast.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ForecastRecord(
        String startTime,
        String publishTime,
        double generation
) {}
