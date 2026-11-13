package com.aerocast.dto;

public record DecisionSimulationResponse(
        double consumerDemandMw,
        double forecastWindMw,
        double netThermalNeededMw,
        double safetyBufferMw,
        double recommendedProcurementMw,
        MetricsResponse metrics
) {}
