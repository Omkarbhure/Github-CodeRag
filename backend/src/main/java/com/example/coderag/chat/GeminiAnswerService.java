package com.example.coderag.chat;

import com.example.coderag.dto.MessageDto;
import com.example.coderag.model.MessageRole;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeminiAnswerService implements AnswerGenerationService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAnswerService.class);

    private final String apiKey;
    private final String chatModel;
    private final RestClient restClient;
    private final com.example.coderag.ratelimit.GeminiQuotaManager quotaManager;

    @Autowired
    public GeminiAnswerService(
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.chat-model:gemini-1.5-flash}") String chatModel,
            @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Autowired(required = false) com.example.coderag.ratelimit.GeminiQuotaManager quotaManager
    ) {
        this.apiKey = apiKey;
        this.chatModel = chatModel;
        this.quotaManager = quotaManager;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(15));
        requestFactory.setReadTimeout(Duration.ofSeconds(90));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public GeminiAnswerService(String apiKey, String chatModel, String baseUrl) {
        this(apiKey, chatModel, baseUrl, (com.example.coderag.ratelimit.GeminiQuotaManager) null);
    }

    // Secondary constructor for testing
    public GeminiAnswerService(String apiKey, String chatModel, RestClient restClient) {
        this(apiKey, chatModel, restClient, null);
    }

    public GeminiAnswerService(String apiKey, String chatModel, RestClient restClient, com.example.coderag.ratelimit.GeminiQuotaManager quotaManager) {
        this.apiKey = apiKey;
        this.chatModel = chatModel;
        this.restClient = restClient;
        this.quotaManager = quotaManager;
    }

    @Override
    public String generateAnswer(String question, String codeContext, List<MessageDto> history) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured. Please set GEMINI_API_KEY environment variable.");
        }

        if (quotaManager != null) {
            quotaManager.acquireQuota();
        }

        String systemInstruction = """
                You are GitHub CodeRAG, an expert AI software engineering assistant.
                Your task is to answer user questions about a codebase strictly grounded in the provided code context.

                CRITICAL CITATION RULES:
                1. Whenever you reference a function, class, logic, or fact from the code, you MUST cite the source inline using EXACTLY this format: [filePath:startLine-endLine].
                   Example: "The authentication filter checks JWT tokens on incoming requests [src/security/JwtAuthFilter.java:35-52]."
                   Example: "Database migrations are configured with Flyway [src/main/resources/application.yml:22-25]."
                2. If the question asks about something not present in the provided code snippets, state honestly:
                   "I do not have enough context in the indexed codebase to answer this question."
                3. Do NOT fabricate, guess, or hallucinate file paths or line numbers. Only cite lines provided in the context blocks.
                """;

        String userPromptWithContext;
        if (codeContext != null && !codeContext.isBlank()) {
            userPromptWithContext = String.format("""
                    CODEBASE CONTEXT:
                    %s

                    USER QUESTION:
                    %s
                    """, codeContext, question);
        } else {
            userPromptWithContext = String.format("""
                    CODEBASE CONTEXT:
                    (No relevant code snippets found in repository index.)

                    USER QUESTION:
                    %s
                    """, question);
        }

        List<ContentItem> contents = new ArrayList<>();

        // Add history turns (limit last 6 messages for focus)
        if (history != null && !history.isEmpty()) {
            int startIdx = Math.max(0, history.size() - 6);
            for (int i = startIdx; i < history.size(); i++) {
                MessageDto msg = history.get(i);
                String role = msg.getRole() == MessageRole.USER ? "user" : "model";
                contents.add(new ContentItem(role, List.of(new PartItem(msg.getContent()))));
            }
        }

        // Add current question with context
        contents.add(new ContentItem("user", List.of(new PartItem(userPromptWithContext))));

        GeminiGenerateRequest request = new GeminiGenerateRequest(
                new SystemInstruction(List.of(new PartItem(systemInstruction))),
                contents
        );

        int maxRetries = 4;
        int attempt = 0;
        long backoffMs = 2000;
        String currentModel = (chatModel != null && !chatModel.isBlank()) ? chatModel : "gemini-3.6-flash";

        while (true) {
            try {
                attempt++;
                final String modelToCall = currentModel;
                GeminiGenerateResponse response = restClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1beta/models/{model}:generateContent")
                                .queryParam("key", apiKey)
                                .build(modelToCall))
                        .body(request)
                        .retrieve()
                        .body(GeminiGenerateResponse.class);

                if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                    return "I was unable to generate an answer at this time.";
                }

                Candidate candidate = response.candidates().get(0);
                if (candidate.content() == null || candidate.content().parts() == null || candidate.content().parts().isEmpty()) {
                    return "I was unable to generate an answer at this time.";
                }

                StringBuilder answer = new StringBuilder();
                for (PartItem part : candidate.content().parts()) {
                    if (part.text() != null) {
                        answer.append(part.text());
                    }
                }

                return answer.toString().trim();

            } catch (HttpClientErrorException e) {
                HttpStatusCode status = e.getStatusCode();
                if (status.value() == 401 || status.value() == 403) {
                    log.error("Gemini API authentication failed ({}): Please check your GEMINI_API_KEY.", status);
                    throw new IllegalStateException("Gemini API authentication failed (" + status.value() + "). Please check your GEMINI_API_KEY configuration.", e);
                }

                if (status.value() == 404 && !"gemini-3.6-flash".equals(currentModel)) {
                    log.warn("Model {} returned 404. Falling back to gemini-3.6-flash", currentModel);
                    currentModel = "gemini-3.6-flash";
                    continue;
                }

                if (status.value() == 429) {
                    if (attempt <= maxRetries) {
                        log.warn("Gemini API rate limit (429) reached during chat generation. Retrying in {}ms (attempt {}/{})", backoffMs, attempt, maxRetries);
                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted during rate limit backoff", ie);
                        }
                        backoffMs *= 2;
                        continue;
                    }
                    throw new RuntimeException("Gemini API rate limit reached. Please retry in a few seconds.", e);
                }

                log.error("Gemini API client error: {} - {}", status, e.getResponseBodyAsString());
                throw new RuntimeException("Gemini API error (" + status.value() + "): " + e.getMessage(), e);

            } catch (HttpServerErrorException e) {
                if (attempt <= maxRetries) {
                    log.warn("Gemini API server error ({}). Retrying in {}ms (attempt {}/{})", e.getStatusCode(), backoffMs, attempt, maxRetries);
                    if (attempt >= 2 && "gemini-3.6-flash".equals(currentModel)) {
                        // Keep on gemini-3.6-flash or fallback if needed
                        currentModel = "gemini-3.6-flash";
                    }
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during server error backoff", ie);
                    }
                    backoffMs *= 2;
                    continue;
                }
                throw new RuntimeException("Gemini service is temporarily busy (" + e.getStatusCode().value() + "). Please retry in a few seconds.", e);

            } catch (org.springframework.web.client.ResourceAccessException e) {
                if (attempt <= maxRetries) {
                    log.warn("Gemini API connection issue ({}). Retrying in {}ms (attempt {}/{})", e.getMessage(), backoffMs, attempt, maxRetries);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during connection backoff", ie);
                    }
                    backoffMs *= 2;
                    continue;
                }
                throw new RuntimeException("Gemini service connection timed out. Please retry.", e);

            } catch (Exception e) {
                if (e instanceof IllegalStateException || (e.getMessage() != null && e.getMessage().contains("GEMINI_API_KEY"))) {
                    throw e;
                }
                log.error("Failed to generate answer with Gemini: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to generate answer: " + e.getMessage(), e);
            }
        }
    }

    public record GeminiGenerateRequest(
            @JsonProperty("systemInstruction") SystemInstruction systemInstruction,
            @JsonProperty("contents") List<ContentItem> contents
    ) {}

    public record SystemInstruction(
            @JsonProperty("parts") List<PartItem> parts
    ) {}

    public record ContentItem(
            @JsonProperty("role") String role,
            @JsonProperty("parts") List<PartItem> parts
    ) {}

    public record PartItem(
            @JsonProperty("text") String text
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeminiGenerateResponse(
            @JsonProperty("candidates") List<Candidate> candidates
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candidate(
            @JsonProperty("content") CandidateContent content
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CandidateContent(
            @JsonProperty("parts") List<PartItem> parts
    ) {}
}
