package com.aiops.kind_guardian.store;

import com.aiops.kind_guardian.faults.FaultSnapshot;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FaultSnapshotStore {

    private final Map<String, FaultSnapshot> sessions = new ConcurrentHashMap<>();

    public void save(FaultSnapshot snapshot) {
        sessions.put(snapshot.sessionId(), snapshot);
    }

    public FaultSnapshot get(String sessionId) {
        return sessions.get(sessionId);
    }

    public void remove(String sessionId) {
        sessions.remove(sessionId);
    }

    public Map<String, FaultSnapshot> getAll() {
        return Map.copyOf(sessions);
    }
}