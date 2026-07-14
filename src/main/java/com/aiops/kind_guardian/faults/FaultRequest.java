package com.aiops.kind_guardian.faults;

public record FaultRequest(
        Topic topic,
        String resourceKind,   // "Deployment", "Service", etc
        String resourceName,
        String namespace,
        String targetField,    // JSON pointer path, e.g. /spec/replicas
        FaultOperation operation,
        String badValue         // ignored for REMOVE
) {}
