package com.aerocast.service;

import com.aerocast.dto.ActualRecord;
import com.aerocast.dto.DailyMetric;
import com.aerocast.dto.ForecastRecord;
import com.aerocast.dto.MetricsResponse;
import com.aerocast.dto.TimeSeriesPoint;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MetricsCalculationService {

    public MetricsResponse calculateMetrics(List<ActualRecord> actuals, List<ForecastRecord> forecasts) {
        if (actuals == null || forecasts == null || actuals.isEmpty() || forecasts.isEmpty()) {
            return new MetricsResponse(0.0, 0.0, 0.0, 0, Collections.emptyList(), Collections.emptyList());
        }

        // Map actuals by startTime
        Map<String, Double> actualMap = new HashMap<>();
        for (ActualRecord a : actuals) {
            actualMap.put(a.startTime(), a.generation());
        }

        // Map forecasts (latest per startTime)
        Map<String, Double> forecastMap = new HashMap<>();
        for (ForecastRecord f : forecasts) {
            forecastMap.put(f.startTime(), f.generation());
        }

        // Intersection of matching timestamps
        List<String> matchedTimes = new ArrayList<>(actualMap.keySet());
        matchedTimes.retainAll(forecastMap.keySet());
        Collections.sort(matchedTimes);

        if (matchedTimes.isEmpty()) {
            return new MetricsResponse(0.0, 0.0, 0.0, 0, Collections.emptyList(), Collections.emptyList());
        }

        List<Double> absoluteErrors = new ArrayList<>();
        List<Double> squaredErrors = new ArrayList<>();
        List<Double> percentageErrors = new ArrayList<>();
        List<TimeSeriesPoint> series = new ArrayList<>();

        // Group by Date for daily breakdown
        Map<String, List<Double>> dailyAbsErrors = new TreeMap<>();
        Map<String, List<Double>> dailySqErrors = new TreeMap<>();
        Map<String, List<Double>> dailyPcntErrors = new TreeMap<>();

        for (String time : matchedTimes) {
            double actual = actualMap.get(time);
            double forecast = forecastMap.get(time);
            double error = forecast - actual;
            double absErr = Math.abs(error);
            double sqErr = error * error;

            absoluteErrors.add(absErr);
            squaredErrors.add(sqErr);

            if (actual > 0.001) {
                percentageErrors.add((absErr / actual) * 100.0);
            }

            series.add(new TimeSeriesPoint(time, actual, forecast, error));

            String date = time.length() >= 10 ? time.substring(0, 10) : time;
            dailyAbsErrors.computeIfAbsent(date, k -> new ArrayList<>()).add(absErr);
            dailySqErrors.computeIfAbsent(date, k -> new ArrayList<>()).add(sqErr);
            if (actual > 0.001) {
                dailyPcntErrors.computeIfAbsent(date, k -> new ArrayList<>()).add((absErr / actual) * 100.0);
            }
        }

        double overallMae = average(absoluteErrors);
        double overallRmse = Math.sqrt(average(squaredErrors));
        double overallMape = average(percentageErrors);

        List<DailyMetric> dailyMetrics = new ArrayList<>();
        for (String date : dailyAbsErrors.keySet()) {
            List<Double> absList = dailyAbsErrors.get(date);
            List<Double> sqList = dailySqErrors.get(date);
            List<Double> pcntList = dailyPcntErrors.getOrDefault(date, Collections.emptyList());

            double dMae = average(absList);
            double dRmse = Math.sqrt(average(sqList));
            double dMape = average(pcntList);

            dailyMetrics.add(new DailyMetric(date, round(dMae, 1), round(dRmse, 1), round(dMape, 1), absList.size()));
        }

        return new MetricsResponse(
                round(overallMae, 1),
                round(overallRmse, 1),
                round(overallMape, 2),
                matchedTimes.size(),
                dailyMetrics,
                series
        );
    }

    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        double sum = 0.0;
        for (double v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    private double round(double val, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(val * factor) / factor;
    }
}
