package com.aerocast.dto;

public record DailyMetric(
        String date,
        double mae,
        double rmse,
        double mape,
        int n
) {}
