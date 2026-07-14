package com.aiops.kind_guardian.components;

import com.aiops.kind_guardian.faults.*;
import com.aiops.kind_guardian.orchestrator.FaultOrchestrator;
import com.aiops.kind_guardian.service.FaultService;
import io.kubernetes.client.openapi.models.V1Deployment;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class K8sFaultTools {

    private final FaultService faultService;
    private final FaultOrchestrator faultOrchestrator;

    public K8sFaultTools(FaultService faultService, FaultOrchestrator faultOrchestrator) {
        this.faultService = faultService;
        this.faultOrchestrator = faultOrchestrator;
    }



    @Tool(description = """
Inject a Kubernetes fault into an existing resource.

ALWAYS use this tool whenever the user asks to:
- inject a fault
- break a deployment
- simulate a Kubernetes failure
- create a CKAD/CKA troubleshooting scenario

Do not explain how to use kubectl.
Do not provide YAML.
Use this tool instead.
""")
    public String injectFault(
            @ToolParam(description = "A short ID you generate to group multiple faults together, e.g. 'combo-1'. Use the SAME value across multiple calls to link them.") String comboId,
            @ToolParam(description = "Topic, e.g. WORKLOADS, STORAGE") String topic,
            @ToolParam(description = "Resource kind, e.g. Deployment") String resourceKind,
            @ToolParam(description = "Resource name") String resourceName,
            @ToolParam(description = "Namespace") String namespace,
            @ToolParam(description = "JSON pointer target field") String targetField,
            @ToolParam(description = "REPLACE or REMOVE") String operation,
            @ToolParam(description = "Broken value (ignored for REMOVE)") String badValue) {

        try {
            FaultRequest request = new FaultRequest(
                    Topic.valueOf(topic), resourceKind, resourceName, namespace,
                    targetField, FaultOperation.valueOf(operation), badValue);

            String sessionId = faultService.injectIntoCombo(comboId, request);
//              String sessionId = faultOrchestrator.injectCombo(comboId, request);
            return "Fault injected under combo '" + comboId + "'. Session ID: " + sessionId
                    + ". Call injectFault again with the same comboId to add more faults, or start validating.";
        } catch (Exception e) {
            return "Failed to inject fault: " + e.getMessage();
        }
    }

    @Tool(description = "Validate whether ALL faults in a combo session have been fixed.")
    public String validateCombo(@ToolParam(description = "Session ID") String sessionId) {
        try {
            ValidationResult result = faultService.validateCombo(sessionId);
            return result.message() + " (elapsed: " + result.secondsElapsed() + "s)";
        } catch (Exception e) {
            return "Validation failed: " + e.getMessage();
        }
    }
}