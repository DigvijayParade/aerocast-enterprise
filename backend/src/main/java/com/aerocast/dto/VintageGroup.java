package com.aerocast.dto;

import java.util.List;

public record VintageGroup(
        String targetDate,
        List<String> publishTimes
) {}
