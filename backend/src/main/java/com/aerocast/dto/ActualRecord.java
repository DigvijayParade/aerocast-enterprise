package com.aerocast.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ActualRecord(
        String startTime,
        double generation
) {}
