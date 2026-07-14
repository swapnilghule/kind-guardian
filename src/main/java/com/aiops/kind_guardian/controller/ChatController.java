package com.aiops.kind_guardian.controller;

import com.aiops.kind_guardian.model.request.ChatRequest;
import com.aiops.kind_guardian.model.response.ChatResponse;
import com.aiops.kind_guardian.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest chatRequest){
        String answer= chatService.chat(chatRequest.getMessage());
        return new ChatResponse(answer);
    }

    @PostMapping(value = "/pods/fix", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> fixPod(@RequestBody ChatRequest request) {
        return chatService.chatStream(request.getMessage());
    }

}
