package com.aerocast.service;

import com.aerocast.dto.ActualRecord;
import com.aerocast.dto.ForecastRecord;
import com.aerocast.dto.MetricsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCalculationServiceTest {

    private MetricsCalculationService service;

    @BeforeEach
    void setUp() {
        service = new MetricsCalculationService();
    }

    @Test
    @DisplayName("Should correctly calculate MAE, RMSE, and MAPE for matching settlement intervals")
    void shouldCalculateAccurateMetrics() {
        List<ActualRecord> actuals = List.of(
                new ActualRecord("2026-01-01T00:00:00Z", 1000.0),
                new ActualRecord("2026-01-01T00:30:00Z", 2000.0)
        );

        List<ForecastRecord> forecasts = List.of(
                new ForecastRecord("2026-01-01T00:00:00Z", "2025-12-31T12:00:00Z", 1200.0), // error +200, 20%
                new ForecastRecord("2026-01-01T00:30:00Z", "2025-12-31T12:00:00Z", 1800.0)  // error -200, 10%
        );

        MetricsResponse response = service.calculateMetrics(actuals, forecasts);

        assertEquals(2, response.n());
        assertEquals(200.0, response.mae(), 0.01);
        assertEquals(200.0, response.rmse(), 0.01);
        assertEquals(15.0, response.mape(), 0.01);
        assertEquals(1, response.daily().size());
        assertEquals(2, response.series().size());
    }

    @Test
    @DisplayName("Should return empty metrics when datasets do not overlap")
    void shouldHandleEmptyOrNonOverlappingData() {
        MetricsResponse response = service.calculateMetrics(List.of(), List.of());
        assertEquals(0, response.n());
        assertEquals(0.0, response.mae());
    }
}
