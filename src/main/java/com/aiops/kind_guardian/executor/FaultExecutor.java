package com.aiops.kind_guardian.executor;

import com.aiops.kind_guardian.faults.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.util.PatchUtils;
import org.springframework.stereotype.Component;

@Component
public class FaultExecutor {

    private final AppsV1Api appsV1Api;
    private final AllowedFieldsConfig allowedFieldsConfig;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public FaultExecutor(AppsV1Api appsV1Api, AllowedFieldsConfig allowedFieldsConfig) {
        this.appsV1Api = appsV1Api;
        this.allowedFieldsConfig = allowedFieldsConfig;
    }

    /** Inject a single fault and return the record needed for later validation. */
    public InjectedFault inject(FaultRequest request) throws Exception {
        if (!allowedFieldsConfig.isAllowed(request.topic(), request.targetField())) {
            throw new IllegalArgumentException(
                    "Field not permitted for topic " + request.topic() + ": " + request.targetField());
        }

        V1Deployment current = appsV1Api.readNamespacedDeployment(
                request.resourceName(), request.namespace()).execute();
        String originalValue = extractValue(current, request.targetField());

        applyPatch(request);

        return new InjectedFault(request, originalValue, false);
    }

    /** Check whether a previously injected fault has since been fixed. */
    public boolean isFixed(InjectedFault fault) throws Exception {
        FaultRequest req = fault.request();
        V1Deployment current = appsV1Api.readNamespacedDeployment(
                req.resourceName(), req.namespace()).execute();
        String currentValue = extractValue(current, req.targetField());
        return !currentValue.equals(req.badValue());
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

        V1Deployment afterPatch = appsV1Api.readNamespacedDeployment(
                request.resourceName(), request.namespace()).execute();
        String actualValue = extractValue(afterPatch, request.targetField());

        if (request.operation() == FaultOperation.REPLACE && !actualValue.equals(request.badValue())) {
            throw new RuntimeException("Patch appeared to succeed but field value is unchanged. "
                    + "Expected: " + request.badValue() + ", actual: " + actualValue);
        }
    }

    private String extractValue(V1Deployment deployment, String jsonPointerPath) {
        JsonNode root = objectMapper.valueToTree(deployment);
        JsonNode value = root.at(jsonPointerPath);
        return value.isMissingNode() ? "<missing>" : value.asText();
    }
}