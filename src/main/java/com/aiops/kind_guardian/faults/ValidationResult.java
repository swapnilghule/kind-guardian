package com.aiops.kind_guardian.faults;

public record ValidationResult(
        boolean fixed,
        String message,
        long secondsElapsed
) {}