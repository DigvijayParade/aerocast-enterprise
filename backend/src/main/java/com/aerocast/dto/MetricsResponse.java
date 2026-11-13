package com.aerocast.dto;

import java.util.List;

public record MetricsResponse(
        double mae,
        double rmse,
        double mape,
        int n,
        List<DailyMetric> daily,
        List<TimeSeriesPoint> series
) {}
