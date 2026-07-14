package com.aiops.kind_guardian.service;


import com.aiops.kind_guardian.components.K8sFaultTools;
import com.aiops.kind_guardian.components.K8sWriteTools;
import lombok.AllArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final K8sWriteTools k8sWriteTools;

    private  final K8sFaultTools k8sFaultTools;

    public ChatService(ChatClient.Builder builder, K8sWriteTools k8sWriteTools, K8sFaultTools k8sFaultTools) {
        this.chatClient = builder.build();
        this.k8sWriteTools = k8sWriteTools;
        this.k8sFaultTools = k8sFaultTools;
    }

    public String chat(String message) {
        return chatClient.prompt()
                .user(message)
                .tools(k8sWriteTools)
                .call()
                .content();
    }

    public Flux<String> chatStream(String message) {
        return chatClient.prompt()
                .user(message)
                .tools(k8sWriteTools, k8sFaultTools)
                .stream()
                .content();
    }

    public String diagnosePod(String podName, Map<String, Object> podInfo, List<String> events) {
        String prompt = """
        You are a Kubernetes troubleshooting assistant. Analyze this pod's status and events,
        then explain what is wrong and suggest a fix. Be concise and specific.

        Pod name: %s

        Pod status:
        %s

        Events:
        %s
        """.formatted(podName, podInfo.toString(), String.join("\n", events));

        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }

}
