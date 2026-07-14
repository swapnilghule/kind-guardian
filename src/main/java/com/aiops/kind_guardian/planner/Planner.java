package com.aiops.kind_guardian.planner;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class Planner {

    public List<FaultNode> planOrder(List<FaultNode> faults) {
        Map<String, FaultNode> byId = new HashMap<>();
        for (FaultNode f : faults) byId.put(f.getId(), f);

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (FaultNode f : faults) inDegree.put(f.getId(), 0);

        for (FaultNode f : faults) {
            for (String dep : f.getDependsOn()) {
                if (!byId.containsKey(dep)) continue; // dependency has no fault, ignore
                adj.computeIfAbsent(dep, k -> new ArrayList<>()).add(f.getId());
                inDegree.merge(f.getId(), 1, Integer::sum);
            }
        }

        Queue<String> ready = new LinkedList<>();
        for (Map.Entry<String, Integer> e : inDegree.entrySet()) {
            if (e.getValue() == 0) ready.add(e.getKey());
        }

        List<FaultNode> ordered = new ArrayList<>();
        while (!ready.isEmpty()) {
            String cur = ready.poll();
            ordered.add(byId.get(cur));
            for (String next : adj.getOrDefault(cur, List.of())) {
                int updated = inDegree.merge(next, -1, Integer::sum);
                if (updated == 0) ready.add(next);
            }
        }

        if (ordered.size() != faults.size()) {
            throw new IllegalStateException("Cycle detected in fault dependency graph — cannot determine safe order");
        }
        return ordered;
    }
}