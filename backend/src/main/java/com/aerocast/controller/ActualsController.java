package com.aerocast.controller;

import com.aerocast.dto.ActualRecord;
import com.aerocast.service.ElexonDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/actuals")
@Tag(name = "Actual Generation", description = "Endpoints for physical wind generation outturn (FUELHH dataset)")
public class ActualsController {

    private final ElexonDataService dataService;

    public ActualsController(ElexonDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping
    @Operation(summary = "Get wind actuals", description = "Returns metered wind generation. Scope can be 'jan2026' or 'live'.")
    public List<ActualRecord> getActuals(@RequestParam(defaultValue = "jan2026") String scope) {
        return dataService.getActuals(scope);
    }
}
