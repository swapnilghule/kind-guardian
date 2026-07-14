package com.aiops.kind_guardian.components;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.custom.V1Patch;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.util.PatchUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class K8sWriteTools {

    private final AppsV1Api appsV1Api;

    public K8sWriteTools(AppsV1Api appsV1Api) throws IOException {
        ApiClient client = Config.defaultClient();
        io.kubernetes.client.openapi.Configuration.setDefaultApiClient(client);
        this.appsV1Api = appsV1Api;
    }

    @Tool(description = "Preview a fix to a deployment's container image WITHOUT applying it. "
            + "Returns the JSON patch that would be sent. Always call this before patchDeploymentImage.")
    public String previewImageFix(
            @ToolParam(description = "Deployment name") String deploymentName,
            @ToolParam(description = "Namespace") String namespace,
            @ToolParam(description = "Container name inside the pod spec") String containerName,
            @ToolParam(description = "New image, e.g. nginx:1.25") String newImage) {

        String patchJson = buildImagePatch(containerName, newImage);
        return "DRY RUN - not applied.\nTarget: deployment/" + deploymentName
                + " in namespace " + namespace
                + "\nPatch to be sent:\n" + patchJson
                + "\nTo apply, call patchDeploymentImage with the same arguments.";
    }

    @Tool(description = "Apply a container image patch to a deployment. Only call this "
            + "AFTER previewImageFix was called and the user has explicitly confirmed.")
    public String patchDeploymentImage(
            @ToolParam(description = "Deployment name") String deploymentName,
            @ToolParam(description = "Namespace") String namespace,
            @ToolParam(description = "Container name inside the pod spec") String containerName,
            @ToolParam(description = "New image, e.g. nginx:1.25") String newImage) {

        String patchJson = buildImagePatch(containerName, newImage);
        try {
            V1Deployment result = PatchUtils.patch(
                    V1Deployment.class,
                    () -> appsV1Api.patchNamespacedDeployment(
                                    deploymentName, namespace, new V1Patch(patchJson))
                            .buildCall(null),
                    V1Patch.PATCH_FORMAT_JSON_PATCH,
                    appsV1Api.getApiClient()
            );
            return "Applied. deployment/" + deploymentName + " in " + namespace + " now uses image " + newImage + ". Pods will roll out shortly - check with K8s get Po.";
        } catch (ApiException e) {
            return "Failed to apply patch: " + e.getResponseBody();
        }
    }

    private String buildImagePatch(String containerName, String newImage) {
        // JSON Patch - assumes containers[0]; for multi-container pods you'd need to find the index by name first
        return "[{\"op\":\"replace\",\"path\":\"/spec/template/spec/containers/0/image\",\"value\":\"" + newImage + "\"}]";
    }
}