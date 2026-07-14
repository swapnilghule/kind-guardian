package com.aiops.kind_guardian.service;


import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.CoreV1EventList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.Config;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class KubernetesService {

    private final CoreV1Api api;

    public KubernetesService() throws IOException {
        ApiClient client = Config.defaultClient(); // reads ~/.kube/config automatically
        Configuration.setDefaultApiClient(client);
        this.api = new CoreV1Api();
    }

    public List<V1Pod> getPods(String namespace) throws ApiException {
        V1PodList list = api.listNamespacedPod(
                namespace
        ).execute();
        return list.getItems();
    }

    public List<Map<String, Object>> getPodDiagnostics(String namespace) throws ApiException {
        V1PodList list = api.listNamespacedPod(namespace).execute();


        return list.getItems().stream().map(pod -> {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("name", pod.getMetadata().getName());
            info.put("phase", pod.getStatus().getPhase());

            List<Map<String, Object>> containerStatuses = new ArrayList<>();
            if (pod.getStatus().getContainerStatuses() != null) {
                for (var cs : pod.getStatus().getContainerStatuses()) {
                    Map<String, Object> c = new LinkedHashMap<>();
                    c.put("name", cs.getName());
                    c.put("ready", cs.getReady());
                    c.put("restartCount", cs.getRestartCount());

                    if (cs.getState() != null) {
                        if (cs.getState().getWaiting() != null) {
                            c.put("state", "waiting");
                            c.put("reason", cs.getState().getWaiting().getReason());
                            c.put("message", cs.getState().getWaiting().getMessage());
                        } else if (cs.getState().getTerminated() != null) {
                            c.put("state", "terminated");
                            c.put("reason", cs.getState().getTerminated().getReason());
                        } else if (cs.getState().getRunning() != null) {
                            c.put("state", "running");
                        }
                    }
                    containerStatuses.add(c);
                }
            }
            info.put("containers", containerStatuses);
            return info;
        }).toList();
    }

    public List<String> getPodEvents(String namespace, String podName) throws ApiException {
        CoreV1EventList events = api.listNamespacedEvent(namespace)
                .fieldSelector("involvedObject.name=" + podName)
                .execute();

        return events.getItems().stream()
                .map(e -> String.format("[%s] %s: %s",
                        e.getReason(), e.getType(), e.getMessage()))
                .toList();
    }
}