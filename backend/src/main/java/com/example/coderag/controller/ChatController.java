package com.example.coderag.controller;

import com.example.coderag.chat.ChatService;
import com.example.coderag.dto.ConversationDto;
import com.example.coderag.dto.MessageDto;
import com.example.coderag.dto.SendMessageRequestDto;
import com.example.coderag.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final com.example.coderag.ratelimit.RateLimiterService rateLimiterService;

    public ChatController(
            ChatService chatService,
            com.example.coderag.ratelimit.RateLimiterService rateLimiterService
    ) {
        this.chatService = chatService;
        this.rateLimiterService = rateLimiterService;
    }

    @PostMapping("/repositories/{id}/conversations")
    public ResponseEntity<ConversationDto> createConversation(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String title = body != null ? body.get("title") : null;
        ConversationDto conversation = chatService.createConversation(principal.getId(), id, title);
        return ResponseEntity.status(HttpStatus.CREATED).body(conversation);
    }

    @GetMapping("/repositories/{id}/conversations")
    public ResponseEntity<List<ConversationDto>> getRepositoryConversations(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        List<ConversationDto> conversations = chatService.getRepositoryConversations(principal.getId(), id);
        return ResponseEntity.ok(conversations);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<List<MessageDto>> getConversationMessages(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id
    ) {
        List<MessageDto> messages = chatService.getConversationMessages(principal.getId(), id);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/conversations/{id}/messages")
    public ResponseEntity<MessageDto> sendMessage(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody SendMessageRequestDto request
    ) {
        rateLimiterService.checkRateLimit(principal.getId(), com.example.coderag.ratelimit.RateLimiterService.ActionType.CHAT);
        MessageDto assistantMessage = chatService.sendMessage(principal.getId(), id, request);
        return ResponseEntity.ok(assistantMessage);
    }
}
