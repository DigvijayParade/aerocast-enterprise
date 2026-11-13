package com.aerocast.dto;

public record DecisionSimulationRequest(
        double consumerDemandMw,
        String publishBefore,
        String scope
) {}
