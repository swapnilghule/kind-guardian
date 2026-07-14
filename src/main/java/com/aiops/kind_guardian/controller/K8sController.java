package com.aiops.kind_guardian.controller;

import com.aiops.kind_guardian.model.request.ChatRequest;
import com.aiops.kind_guardian.service.ChatService;
import com.aiops.kind_guardian.service.KubernetesService;
import io.kubernetes.client.openapi.ApiException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/k8s")
public class K8sController {

    private final KubernetesService k8sService;
    private  final ChatService chatService;

    public K8sController(KubernetesService k8sService, ChatService chatService) {
        this.k8sService = k8sService;
        this.chatService = chatService;
    }

    @GetMapping("/pods")
    public List<String> listPods() throws ApiException {
        return k8sService.getPods("default").stream()
                .map(pod -> pod.getMetadata().getName())
                .toList();
    }

    @GetMapping("/pods/diagnostics")
    public List<Map<String, Object>> podDiagnostics(@RequestParam(defaultValue = "default") String namespace) throws ApiException {
        return k8sService.getPodDiagnostics(namespace);
    }

    @GetMapping("/pods/diagnose")
    public String diagnosePod(@RequestParam String podName,
                              @RequestParam(defaultValue = "default") String namespace) throws ApiException {
        List<Map<String, Object>> pods = k8sService.getPodDiagnostics(namespace);
        Map<String, Object> pod = pods.stream()
                .filter(p -> p.get("name").equals(podName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Pod not found: " + podName));

        List<String> events = k8sService.getPodEvents(namespace, podName);
        return chatService.diagnosePod(podName, pod, events);
    }

//    @PostMapping(value = "/pods/fix", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<String> fixPod(@RequestBody ChatRequest request) {
//        return chatService.chatStream(request.getMessage());
//    }


}
