package com.aerocast.controller;

import com.aerocast.dto.DecisionSimulationRequest;
import com.aerocast.dto.DecisionSimulationResponse;
import com.aerocast.service.ProcurementDecisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/decision")
@Tag(name = "Decision Desk", description = "Wholesale power purchase and thermal balancing recommendation engine")
public class DecisionController {

    private final ProcurementDecisionService decisionService;

    public DecisionController(ProcurementDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @PostMapping("/simulate")
    @Operation(summary = "Simulate procurement order", description = "Calculates net thermal power required and recommended safety buffer.")
    public DecisionSimulationResponse simulateProcurement(@RequestBody DecisionSimulationRequest request) {
        return decisionService.simulate(request);
    }
}
