package com.aiops.kind_guardian.faults;

import java.time.Instant;
import java.util.List;

public record FaultSnapshot(
        String sessionId,
        List<InjectedFault> faults,
        Instant injectedAt
) {}