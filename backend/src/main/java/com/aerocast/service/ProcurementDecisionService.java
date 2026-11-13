package com.aerocast.service;

import com.aerocast.dto.ActualRecord;
import com.aerocast.dto.DecisionSimulationRequest;
import com.aerocast.dto.DecisionSimulationResponse;
import com.aerocast.dto.ForecastRecord;
import com.aerocast.dto.MetricsResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcurementDecisionService {

    private final ElexonDataService dataService;
    private final MetricsCalculationService metricsService;

    public ProcurementDecisionService(ElexonDataService dataService, MetricsCalculationService metricsService) {
        this.dataService = dataService;
        this.metricsService = metricsService;
    }

    public DecisionSimulationResponse simulate(DecisionSimulationRequest request) {
        String scope = request.scope() != null ? request.scope() : "jan2026";
        List<ActualRecord> actuals = dataService.getActuals(scope);
        List<ForecastRecord> forecasts;

        if (request.publishBefore() != null && !request.publishBefore().isBlank()) {
            forecasts = dataService.getForecastsByVintage(request.publishBefore(), scope);
        } else {
            forecasts = dataService.getLatestForecasts(scope);
        }

        MetricsResponse metrics = metricsService.calculateMetrics(actuals, forecasts);

        double avgForecast = forecasts.isEmpty() ? 0.0 :
                forecasts.stream().mapToDouble(ForecastRecord::generation).average().orElse(0.0);

        double netThermal = Math.max(0, request.consumerDemandMw() - avgForecast);
        double safetyBuffer = metrics.mae() > 0 ? metrics.mae() : 1764.0;
        double recommended = netThermal + safetyBuffer;

        return new DecisionSimulationResponse(
                request.consumerDemandMw(),
                Math.round(avgForecast),
                Math.round(netThermal),
                Math.round(safetyBuffer),
                Math.round(recommended),
                metrics
        );
    }
}
