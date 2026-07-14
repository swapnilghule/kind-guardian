package com.aiops.kind_guardian.planner;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlannerTest {

    private final Planner planner = new Planner();

    @Test
    void independentFaults_anyOrderValid() {
        List<FaultNode> faults = List.of(
                new FaultNode("f1", "Deployment", List.of()),
                new FaultNode("f2", "Service", List.of())
        );
        List<FaultNode> ordered = planner.planOrder(faults);
        assertEquals(2, ordered.size());
    }

    @Test
    void dependentFaults_configMapFixedBeforeDeployment() {
        // f2 (Deployment) depends on f1 (ConfigMap)
        List<FaultNode> faults = List.of(
                new FaultNode("f2", "Deployment", List.of("f1")),
                new FaultNode("f1", "ConfigMap", List.of())
        );
        List<FaultNode> ordered = planner.planOrder(faults);

        int idxF1 = indexOf(ordered, "f1");
        int idxF2 = indexOf(ordered, "f2");
        assertTrue(idxF1 < idxF2, "ConfigMap fix must come before Deployment fix");
    }

    @Test
    void cyclicDependency_throwsException() {
        List<FaultNode> faults = List.of(
                new FaultNode("f1", "Deployment", List.of("f2")),
                new FaultNode("f2", "Service", List.of("f1"))
        );
        assertThrows(IllegalStateException.class, () -> planner.planOrder(faults));
    }

    private int indexOf(List<FaultNode> list, String id) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(id)) return i;
        }
        return -1;
    }
}