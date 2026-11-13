package com.aerocast.controller;

import com.aerocast.dto.ActualRecord;
import com.aerocast.dto.ForecastRecord;
import com.aerocast.dto.MetricsResponse;
import com.aerocast.service.ElexonDataService;
import com.aerocast.service.MetricsCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@Tag(name = "Forecast Accuracy Metrics", description = "Mathematical error evaluations: MAE, RMSE, MAPE and daily breakdowns")
public class MetricsController {

    private final ElexonDataService dataService;
    private final MetricsCalculationService metricsService;

    public MetricsController(ElexonDataService dataService, MetricsCalculationService metricsService) {
        this.dataService = dataService;
        this.metricsService = metricsService;
    }

    @GetMapping
    @Operation(summary = "Calculate forecast error metrics", description = "Quantifies forecast accuracy across matched settlement points.")
    public MetricsResponse getMetrics(@RequestParam(defaultValue = "jan2026") String scope) {
        List<ActualRecord> actuals = dataService.getActuals(scope);
        List<ForecastRecord> forecasts = dataService.getLatestForecasts(scope);
        return metricsService.calculateMetrics(actuals, forecasts);
    }
}
