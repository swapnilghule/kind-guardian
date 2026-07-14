package com.aiops.kind_guardian.faults;

public record InjectedFault(
        FaultRequest request,
        String originalValue,
        boolean resolved   // tracked individually within the combo
) {}