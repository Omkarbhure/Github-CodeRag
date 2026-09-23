package com.example.coderag.vector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;

@Service
public class GeminiEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(GeminiEmbeddingService.class);

    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final RestClient restClient;
    private final com.example.coderag.ratelimit.GeminiQuotaManager quotaManager;

    @org.springframework.beans.factory.annotation.Autowired
    public GeminiEmbeddingService(
            @Value("${app.gemini.api-key:}") String apiKey,
            @Value("${app.gemini.embedding-model:text-embedding-004}") String model,
            @Value("${app.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @org.springframework.beans.factory.annotation.Autowired(required = false) com.example.coderag.ratelimit.GeminiQuotaManager quotaManager
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.quotaManager = quotaManager;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public GeminiEmbeddingService(String apiKey, String model, String baseUrl) {
        this(apiKey, model, baseUrl, (com.example.coderag.ratelimit.GeminiQuotaManager) null);
    }

    // Secondary constructor for testing with custom RestClient
    public GeminiEmbeddingService(String apiKey, String model, String baseUrl, RestClient restClient) {
        this(apiKey, model, baseUrl, restClient, null);
    }

    public GeminiEmbeddingService(String apiKey, String model, String baseUrl, RestClient restClient, com.example.coderag.ratelimit.GeminiQuotaManager quotaManager) {
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.restClient = restClient;
        this.quotaManager = quotaManager;
    }

    @Override
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new float[768]; // default zero vector for empty text
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured. Please set GEMINI_API_KEY environment variable.");
        }

        if (quotaManager != null) {
            quotaManager.acquireQuota();
        }

        String fullModelPath = model.startsWith("models/") ? model : "models/" + model;
        GeminiEmbedRequest request = new GeminiEmbedRequest(
                fullModelPath,
                new Content(List.of(new Part(text))),
                768
        );

        int maxRetries = 6;
        int attempt = 0;
        long backoffMs = 1500;

        while (true) {
            try {
                attempt++;
                GeminiEmbedResponse response = restClient.post()
                        .uri(uriBuilder -> uriBuilder
                                .path("/v1beta/models/{model}:embedContent")
                                .queryParam("key", apiKey)
                                .build(model))
                        .body(request)
                        .retrieve()
                        .body(GeminiEmbedResponse.class);

                if (response == null || response.embedding() == null || response.embedding().values() == null) {
                    throw new RuntimeException("Empty or invalid embedding response from Gemini API");
                }

                List<Float> values = response.embedding().values();
                float[] vector = new float[values.size()];
                for (int i = 0; i < values.size(); i++) {
                    vector[i] = values.get(i);
                }
                return vector;

            } catch (HttpClientErrorException e) {
                HttpStatusCode status = e.getStatusCode();
                if (status.value() == 401 || status.value() == 403) {
                    log.error("Gemini API authentication failed ({}): Please check your GEMINI_API_KEY.", status);
                    throw new IllegalStateException("Gemini API authentication failed (" + status.value() + "). Please check your GEMINI_API_KEY configuration.", e);
                }

                if (status.value() == 429) {
                    if (attempt <= maxRetries) {
                        log.warn("Gemini API rate limit (429) reached. Retrying in {}ms (attempt {}/{})", backoffMs, attempt, maxRetries);
                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted during rate limit backoff", ie);
                        }
                        backoffMs *= 2;
                        continue;
                    }
                    throw new RuntimeException("Gemini API rate limit exceeded after " + maxRetries + " retries.", e);
                }

                log.error("Gemini API client error: {} - {}", status, e.getResponseBodyAsString());
                throw new RuntimeException("Gemini API client error (" + status.value() + "): " + e.getMessage(), e);

            } catch (HttpServerErrorException e) {
                if (attempt <= maxRetries) {
                    log.warn("Gemini API server error ({}). Retrying in {}ms (attempt {}/{})", e.getStatusCode(), backoffMs, attempt, maxRetries);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during server error backoff", ie);
                    }
                    backoffMs *= 2;
                    continue;
                }
                throw new RuntimeException("Gemini API server error (" + e.getStatusCode() + "): " + e.getMessage(), e);

            } catch (Exception e) {
                if (e instanceof IllegalStateException || e instanceof RuntimeException && e.getMessage().contains("GEMINI_API_KEY")) {
                    throw e;
                }
                log.error("Failed to generate embedding with Gemini: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to generate embedding: " + e.getMessage(), e);
            }
        }
    }

    public record GeminiEmbedRequest(
            @JsonProperty("model") String model,
            @JsonProperty("content") Content content,
            @JsonProperty("outputDimensionality") Integer outputDimensionality
    ) {
        public GeminiEmbedRequest(String model, Content content) {
            this(model, content, 768);
        }
    }

    public record Content(
            @JsonProperty("parts") List<Part> parts
    ) {}

    public record Part(
            @JsonProperty("text") String text
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeminiEmbedResponse(
            @JsonProperty("embedding") EmbeddingData embedding
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmbeddingData(
            @JsonProperty("values") List<Float> values
    ) {}
}
