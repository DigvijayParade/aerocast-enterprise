package com.aerocast.controller;

import com.aerocast.dto.ForecastRecord;
import com.aerocast.dto.VintageGroup;
import com.aerocast.service.ElexonDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/forecast")
@Tag(name = "Wind Forecasts", description = "Endpoints for wind generation forecasts (WINDFOR dataset)")
public class ForecastController {

    private final ElexonDataService dataService;

    public ForecastController(ElexonDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest forecasts", description = "Returns most recent forecast per settlement timestamp.")
    public List<ForecastRecord> getLatestForecasts(@RequestParam(defaultValue = "jan2026") String scope) {
        return dataService.getLatestForecasts(scope);
    }

    @GetMapping("/vintages")
    @Operation(summary = "Get forecast vintages", description = "Returns publication batches grouped by target delivery date.")
    public List<VintageGroup> getForecastVintages(@RequestParam(defaultValue = "jan2026") String scope) {
        return dataService.getForecastVintages(scope);
    }

    @GetMapping("/by-vintage")
    @Operation(summary = "Get forecast by vintage cutoff", description = "Simulates forecast available strictly before a given ISO datetime.")
    public List<ForecastRecord> getForecastByVintage(
            @RequestParam String publishBefore,
            @RequestParam(defaultValue = "jan2026") String scope) {
        return dataService.getForecastsByVintage(publishBefore, scope);
    }
}
