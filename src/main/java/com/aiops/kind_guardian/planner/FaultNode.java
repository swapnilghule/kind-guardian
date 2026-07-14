package com.aiops.kind_guardian.planner;

import com.aiops.kind_guardian.faults.FaultRequest;
import java.util.List;

public class FaultNode {
    private final String id;
    private final FaultRequest request;
    private final List<String> dependsOn;

    public FaultNode(String id, FaultRequest request, List<String> dependsOn) {
        this.id = id;
        this.request = request;
        this.dependsOn = dependsOn;
    }

    public String getId() { return id; }
    public FaultRequest getRequest() { return request; }
    public List<String> getDependsOn() { return dependsOn; }
}