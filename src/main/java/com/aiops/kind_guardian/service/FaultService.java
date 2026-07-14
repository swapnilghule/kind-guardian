package com.aiops.kind_guardian.service;


import com.aiops.kind_guardian.faults.*;
import com.aiops.kind_guardian.store.FaultSnapshotStore;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.util.PatchUtils;
import io.kubernetes.client.custom.V1Patch;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.openapi.ApiException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FaultService {

    private final AppsV1Api appsV1Api;
    private final AllowedFieldsConfig allowedFieldsConfig;
    private final FaultSnapshotStore snapshotStore;


    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    public FaultService(AppsV1Api appsV1Api,
                        AllowedFieldsConfig allowedFieldsConfig,
                        FaultSnapshotStore snapshotStore) {
        this.appsV1Api = appsV1Api;
        this.allowedFieldsConfig = allowedFieldsConfig;
        this.snapshotStore = snapshotStore;
    }

    public String injectCombo(List<FaultRequest> requests) throws Exception {
        List<InjectedFault> injected = new ArrayList<>();

        for (FaultRequest request : requests) {
            if (!allowedFieldsConfig.isAllowed(request.topic(), request.targetField())) {
                throw new IllegalArgumentException(
                        "Field not permitted for topic " + request.topic() + ": " + request.targetField());
            }

            V1Deployment current = appsV1Api.readNamespacedDeployment(
                    request.resourceName(), request.namespace()).execute();
            String originalValue = extractValue(current, request.targetField());

            applyPatch(request);

            injected.add(new InjectedFault(request, originalValue, false));
        }

        String sessionId = UUID.randomUUID().toString();
        snapshotStore.save(new FaultSnapshot(sessionId, injected, Instant.now()));
        return sessionId;
    }

    private void applyPatch(FaultRequest request) throws Exception {
        String patchJson = request.operation() == FaultOperation.REMOVE
                ? "[{\"op\":\"remove\",\"path\":\"%s\"}]".formatted(request.targetField())
                : "[{\"op\":\"replace\",\"path\":\"%s\",\"value\":\"%s\"}]"
                .formatted(request.targetField(), request.badValue());

        try {
            PatchUtils.patch(V1Deployment.class,
                    () -> appsV1Api.patchNamespacedDeployment(
                                    request.resourceName(), request.namespace(), new V1Patch(patchJson))
                            .buildCall(null),
                    V1Patch.PATCH_FORMAT_JSON_PATCH, appsV1Api.getApiClient());
        } catch (ApiException e) {
            throw new RuntimeException("Patch API call failed: " + e.getResponseBody(), e);
        }

        // Verify the patch actually took effect - don't trust a clean return alone
        V1Deployment afterPatch = appsV1Api.readNamespacedDeployment(
                request.resourceName(), request.namespace()).execute();
        String actualValue = extractValue(afterPatch, request.targetField());

        if (request.operation() == FaultOperation.REPLACE && !actualValue.equals(request.badValue())) {
            throw new RuntimeException("Patch appeared to succeed but field value is unchanged. "
                    + "Expected: " + request.badValue() + ", actual: " + actualValue
                    + ". Check that targetField path is valid JSON Pointer syntax and the field/array exists.");
        }
    }

    private String extractValue(V1Deployment deployment, String jsonPointerPath) {
        JsonNode root = objectMapper.valueToTree(deployment);
        JsonNode value = root.at(jsonPointerPath);
        if (value.isMissingNode()) {
            return "<missing>";
        }
        return value.asText();
    }

    public ValidationResult validateCombo(String sessionId) throws Exception {
        FaultSnapshot snapshot = snapshotStore.get(sessionId);
        if (snapshot == null) {
            return new ValidationResult(false, "No active session found.", 0);
        }

        List<InjectedFault> updated = new ArrayList<>();
        int fixedCount = 0;

        for (InjectedFault fault : snapshot.faults()) {
            FaultRequest req = fault.request();
            V1Deployment current = appsV1Api.readNamespacedDeployment(
                    req.resourceName(), req.namespace()).execute();
            String currentValue = extractValue(current, req.targetField());

            boolean thisFixed = !currentValue.equals(req.badValue());
            if (thisFixed) fixedCount++;
            updated.add(new InjectedFault(req, fault.originalValue(), thisFixed));
        }

        boolean allFixed = fixedCount == updated.size();
        long elapsed = Instant.now().getEpochSecond() - snapshot.injectedAt().getEpochSecond();

        String message = allFixed
                ? "All " + updated.size() + " issues fixed!"
                : fixedCount + "/" + updated.size() + " issues fixed so far.";

        if (allFixed) {
            snapshotStore.remove(sessionId);
        } else {
            // save progress so partial fixes aren't lost
            snapshotStore.save(new FaultSnapshot(sessionId, updated, snapshot.injectedAt()));
        }

        return new ValidationResult(allFixed, message, elapsed);
    }

    private final Map<String, String> comboToSessionId = new ConcurrentHashMap<>();

    public String injectIntoCombo(String comboId, FaultRequest request) throws Exception {
//        if (!allowedFieldsConfig.isAllowed(request.topic(), request.targetField())) {
//            throw new IllegalArgumentException(
//                    "Field not permitted for topic " + request.topic() + ": " + request.targetField());
//        }

        V1Deployment current = appsV1Api.readNamespacedDeployment(
                request.resourceName(), request.namespace()).execute();
        String originalValue = extractValue(current, request.targetField());

        applyPatch(request);

        String sessionId = comboToSessionId.computeIfAbsent(comboId, k -> UUID.randomUUID().toString());
        FaultSnapshot existing = snapshotStore.get(sessionId);

        List<InjectedFault> faults = existing == null
                ? new ArrayList<>()
                : new ArrayList<>(existing.faults());
        faults.add(new InjectedFault(request, originalValue, false));

        snapshotStore.save(new FaultSnapshot(sessionId, faults,
                existing == null ? Instant.now() : existing.injectedAt()));

        return sessionId;
    }

}