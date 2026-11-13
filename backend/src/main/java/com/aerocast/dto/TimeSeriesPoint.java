package com.aerocast.dto;

public record TimeSeriesPoint(
        String startTime,
        Double actual,
        Double forecast,
        Double error
) {}
