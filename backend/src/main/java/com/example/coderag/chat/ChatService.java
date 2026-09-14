package com.example.coderag.chat;

import com.example.coderag.dto.ConversationDto;
import com.example.coderag.dto.MessageDto;
import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.dto.SendMessageRequestDto;
import com.example.coderag.exception.ResourceNotFoundException;
import com.example.coderag.model.Conversation;
import com.example.coderag.model.GitHubRepository;
import com.example.coderag.model.Message;
import com.example.coderag.model.MessageRole;
import com.example.coderag.repository.ConversationRepository;
import com.example.coderag.repository.GitHubRepositoryRepository;
import com.example.coderag.repository.MessageRepository;
import com.example.coderag.service.HybridSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final GitHubRepositoryRepository repositoryRepository;
    private final HybridSearchService hybridSearchService;
    private final ContextBuilder contextBuilder;
    private final AnswerGenerationService answerGenerationService;

    public ChatService(
            ConversationRepository conversationRepository,
            MessageRepository messageRepository,
            GitHubRepositoryRepository repositoryRepository,
            HybridSearchService hybridSearchService,
            ContextBuilder contextBuilder,
            AnswerGenerationService answerGenerationService
    ) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.repositoryRepository = repositoryRepository;
        this.hybridSearchService = hybridSearchService;
        this.contextBuilder = contextBuilder;
        this.answerGenerationService = answerGenerationService;
    }

    @Transactional
    public ConversationDto createConversation(UUID userId, UUID repositoryId, String title) {
        GitHubRepository repo = repositoryRepository.findByIdAndOwnerId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        String resolvedTitle = (title != null && !title.isBlank()) ? title : "New Conversation";
        Conversation conversation = Conversation.builder()
                .repositoryId(repo.getId())
                .userId(userId)
                .title(resolvedTitle)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        conversation = conversationRepository.save(conversation);
        log.info("Created conversation {} for user {} on repo {}", conversation.getId(), userId, repo.getFullName());
        return ConversationDto.fromEntity(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getRepositoryConversations(UUID userId, UUID repositoryId) {
        repositoryRepository.findByIdAndOwnerId(repositoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found with id: " + repositoryId));

        return conversationRepository.findByRepositoryIdAndUserIdOrderByUpdatedAtDesc(repositoryId, userId)
                .stream()
                .map(ConversationDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getConversationMessages(UUID userId, UUID conversationId) {
        conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(MessageDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageDto sendMessage(UUID userId, UUID conversationId, SendMessageRequestDto request) {
        Conversation conversation = conversationRepository.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found with id: " + conversationId));

        String question = request.getQuestion().trim();

        // 1. Save user message
        Message userMessage = Message.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.USER)
                .content(question)
                .createdAt(OffsetDateTime.now())
                .build();
        messageRepository.save(userMessage);

        // Fetch prior message history (excluding current user message for generation)
        List<MessageDto> priorMessages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversation.getId())
                .stream()
                .filter(m -> !m.getId().equals(userMessage.getId()))
                .map(MessageDto::fromEntity)
                .collect(Collectors.toList());

        // 2. Retrieve top-8 relevant chunks via Hybrid Search (Vector + PostgreSQL Keyword Fusion)
        List<SearchResultDto> searchResults = hybridSearchService.search(conversation.getRepositoryId(), question, 8);

        // 3. Assemble formatted context with [filePath:lines] headers
        String codeContext = contextBuilder.buildContext(searchResults);

        // 4. Generate grounded LLM answer
        String answer = answerGenerationService.generateAnswer(question, codeContext, priorMessages);

        // 5. Save assistant message
        Message assistantMessage = Message.builder()
                .conversationId(conversation.getId())
                .role(MessageRole.ASSISTANT)
                .content(answer)
                .createdAt(OffsetDateTime.now())
                .build();
        assistantMessage = messageRepository.save(assistantMessage);

        // 6. Update conversation timestamp and title if generic
        if ("New Conversation".equals(conversation.getTitle())) {
            String snippetTitle = question.length() > 40 ? question.substring(0, 37) + "..." : question;
            conversation.setTitle(snippetTitle);
        }
        conversation.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conversation);

        log.info("Processed RAG chat message for conversation {} ({} search results retrieved)", conversation.getId(), searchResults.size());
        return MessageDto.fromEntity(assistantMessage);
    }
}
