package com.example.coderag.controller;

import com.example.coderag.chat.ChatService;
import com.example.coderag.dto.ConversationDto;
import com.example.coderag.dto.MessageDto;
import com.example.coderag.dto.SendMessageRequestDto;
import com.example.coderag.model.AuthProvider;
import com.example.coderag.model.MessageRole;
import com.example.coderag.model.User;
import com.example.coderag.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    private UserPrincipal testUserPrincipal;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@example.com")
                .authProvider(AuthProvider.LOCAL)
                .build();
        testUserPrincipal = UserPrincipal.create(user);
    }

    @Test
    void createConversation_ShouldReturn201() throws Exception {
        UUID repoId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        ConversationDto convDto = new ConversationDto(
                convId, repoId, userId, "Auth Questions", OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(chatService.createConversation(eq(userId), eq(repoId), any())).thenReturn(convDto);

        mockMvc.perform(post("/api/repositories/" + repoId + "/conversations")
                        .with(user(testUserPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Auth Questions\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(convId.toString()))
                .andExpect(jsonPath("$.title").value("Auth Questions"));
    }

    @Test
    void getRepositoryConversations_ShouldReturn200() throws Exception {
        UUID repoId = UUID.randomUUID();
        UUID convId = UUID.randomUUID();

        ConversationDto convDto = new ConversationDto(
                convId, repoId, userId, "Test Conversation", OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(chatService.getRepositoryConversations(eq(userId), eq(repoId))).thenReturn(List.of(convDto));

        mockMvc.perform(get("/api/repositories/" + repoId + "/conversations")
                        .with(user(testUserPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(convId.toString()));
    }

    @Test
    void getConversationMessages_ShouldReturn200() throws Exception {
        UUID convId = UUID.randomUUID();

        MessageDto msgDto = new MessageDto(
                UUID.randomUUID(), convId, MessageRole.USER, "How does auth work?", OffsetDateTime.now()
        );

        when(chatService.getConversationMessages(eq(userId), eq(convId))).thenReturn(List.of(msgDto));

        mockMvc.perform(get("/api/conversations/" + convId + "/messages")
                        .with(user(testUserPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("How does auth work?"))
                .andExpect(jsonPath("$[0].role").value("USER"));
    }

    @Test
    void sendMessage_ShouldReturn200AndAssistantMessage() throws Exception {
        UUID convId = UUID.randomUUID();
        SendMessageRequestDto request = new SendMessageRequestDto("How does auth work?");

        MessageDto assistantDto = new MessageDto(
                UUID.randomUUID(), convId, MessageRole.ASSISTANT,
                "Auth is handled in AuthService [src/AuthService.java:10-30].", OffsetDateTime.now()
        );

        when(chatService.sendMessage(eq(userId), eq(convId), any(SendMessageRequestDto.class))).thenReturn(assistantDto);

        mockMvc.perform(post("/api/conversations/" + convId + "/messages")
                        .with(user(testUserPrincipal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Auth is handled in AuthService [src/AuthService.java:10-30]."))
                .andExpect(jsonPath("$.role").value("ASSISTANT"));
    }
}
