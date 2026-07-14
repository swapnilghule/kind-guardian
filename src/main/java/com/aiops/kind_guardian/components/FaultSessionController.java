package com.aiops.kind_guardian.components;


import com.aiops.kind_guardian.faults.FaultSnapshot;
import com.aiops.kind_guardian.store.FaultSnapshotStore;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/sessions")
public class FaultSessionController {

    private final FaultSnapshotStore snapshotStore;

    public FaultSessionController(FaultSnapshotStore snapshotStore) {
        this.snapshotStore = snapshotStore;
    }

    @GetMapping
    public Map<String, FaultSnapshot> listActiveSessions() {
        return snapshotStore.getAll();
    }
}