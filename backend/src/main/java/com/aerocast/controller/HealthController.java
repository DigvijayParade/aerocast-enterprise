package com.aerocast.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Service operational status")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health check", description = "Returns service health status")
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "framework", "Spring Boot 3.2.5",
                "app", "AeroCast UK Enterprise"
        );
    }
}
