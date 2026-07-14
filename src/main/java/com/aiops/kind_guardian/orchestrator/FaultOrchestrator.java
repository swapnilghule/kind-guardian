package com.aiops.kind_guardian.orchestrator;

import com.aiops.kind_guardian.executor.FaultExecutor;
import com.aiops.kind_guardian.faults.InjectedFault;
import com.aiops.kind_guardian.faults.ValidationResult;
import com.aiops.kind_guardian.planner.FaultNode;
import com.aiops.kind_guardian.planner.Planner;
import com.aiops.kind_guardian.faults.FaultSnapshot;
import com.aiops.kind_guardian.store.FaultSnapshotStore;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class FaultOrchestrator {

    private final Planner planner;
    private final FaultExecutor executor;
    private final FaultSnapshotStore snapshotStore;

    public FaultOrchestrator(Planner planner, FaultExecutor executor, FaultSnapshotStore snapshotStore) {
        this.planner = planner;
        this.executor = executor;
        this.snapshotStore = snapshotStore;
    }

    /** Plan safe order, then inject all faults in a new session. */
    public String injectCombo(List<FaultNode> requestedFaults) throws Exception {
        List<FaultNode> ordered = planner.planOrder(requestedFaults);

        List<InjectedFault> injected = new ArrayList<>();
        for (FaultNode node : ordered) {
            injected.add(executor.inject(node.getRequest()));
        }

        String sessionId = UUID.randomUUID().toString();
        snapshotStore.save(new FaultSnapshot(sessionId, injected, Instant.now()));
        return sessionId;
    }

    /** Validate all faults in a session; keep session open until every fault is fixed. */
    public ValidationResult validateCombo(String sessionId) throws Exception {
        FaultSnapshot snapshot = snapshotStore.get(sessionId);
        if (snapshot == null) {
            return new ValidationResult(false, "No active session found.", 0);
        }

        List<InjectedFault> updated = new ArrayList<>();
        int fixedCount = 0;

        for (InjectedFault fault : snapshot.faults()) {
            boolean thisFixed = executor.isFixed(fault);
            if (thisFixed) fixedCount++;
            updated.add(new InjectedFault(fault.request(), fault.originalValue(), thisFixed));
        }

        boolean allFixed = fixedCount == updated.size();
        long elapsed = Instant.now().getEpochSecond() - snapshot.injectedAt().getEpochSecond();

        String message = allFixed
                ? "All " + updated.size() + " issues fixed!"
                : fixedCount + "/" + updated.size() + " issues fixed so far.";

        if (allFixed) {
            snapshotStore.remove(sessionId);
        } else {
            snapshotStore.save(new FaultSnapshot(sessionId, updated, snapshot.injectedAt()));
        }

        return new ValidationResult(allFixed, message, elapsed);
    }
}