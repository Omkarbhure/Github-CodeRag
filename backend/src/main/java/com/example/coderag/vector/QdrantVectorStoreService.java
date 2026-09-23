package com.example.coderag.vector;

import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.model.CodeChunk;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QdrantVectorStoreService implements VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(QdrantVectorStoreService.class);

    private final String collectionName;
    private final RestClient restClient;

    @org.springframework.beans.factory.annotation.Autowired
    public QdrantVectorStoreService(
            @Value("${app.qdrant.url:http://localhost:6333}") String qdrantUrl,
            @Value("${app.qdrant.collection-name:code_chunks}") String collectionName,
            @Value("${app.qdrant.api-key:}") String qdrantApiKey
    ) {
        this.collectionName = collectionName;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        var builder = RestClient.builder()
                .baseUrl(qdrantUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

        if (qdrantApiKey != null && !qdrantApiKey.trim().isEmpty()) {
            builder.defaultHeader("api-key", qdrantApiKey.trim());
        }

        this.restClient = builder.build();
    }

    // Secondary constructor for testing
    public QdrantVectorStoreService(String collectionName, RestClient restClient) {
        this.collectionName = collectionName;
        this.restClient = restClient;
    }

    @Override
    public void ensureCollectionExists() {
        try {
            restClient.get()
                    .uri("/collections/{name}", collectionName)
                    .retrieve()
                    .toBodilessEntity();
            log.debug("Qdrant collection '{}' already exists.", collectionName);
        } catch (HttpClientErrorException.NotFound e) {
            log.info("Creating Qdrant collection '{}' with dimension 768 and Cosine distance", collectionName);
            Map<String, Object> body = Map.of(
                    "vectors", Map.of(
                            "size", 768,
                            "distance", "Cosine"
                    )
            );
            restClient.put()
                    .uri("/collections/{name}", collectionName)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            // Create index on repositoryId keyword
            try {
                Map<String, Object> indexBody = Map.of(
                        "field_name", "repositoryId",
                        "field_schema", "keyword"
                );
                restClient.put()
                        .uri("/collections/{name}/index", collectionName)
                        .body(indexBody)
                        .retrieve()
                        .toBodilessEntity();
                log.info("Created payload keyword index on 'repositoryId' for collection '{}'", collectionName);
            } catch (Exception ex) {
                log.warn("Could not create payload index on repositoryId (may already exist): {}", ex.getMessage());
            }
        } catch (Exception e) {
            log.error("Failed to connect to Qdrant or check collection: {}", e.getMessage(), e);
            throw new RuntimeException("Qdrant vector database unreachable at configured URL: " + e.getMessage(), e);
        }
    }

    @Override
    public void upsert(CodeChunk chunk, float[] vector) {
        upsertBatch(List.of(chunk), List.of(vector));
    }

    @Override
    public void upsertBatch(List<CodeChunk> chunks, List<float[]> vectors) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        ensureCollectionExists();

        List<Map<String, Object>> points = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            CodeChunk chunk = chunks.get(i);
            float[] vector = vectors.get(i);

            List<Float> floatList = new ArrayList<>(vector.length);
            for (float f : vector) {
                floatList.add(f);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("id", chunk.getId().toString());
            payload.put("repositoryId", chunk.getRepositoryId().toString());
            if (chunk.getRepositoryFileId() != null) {
                payload.put("repositoryFileId", chunk.getRepositoryFileId().toString());
            }
            payload.put("filePath", chunk.getFilePath());
            payload.put("startLine", chunk.getStartLine());
            payload.put("endLine", chunk.getEndLine());
            payload.put("chunkIndex", chunk.getChunkIndex());
            payload.put("commitSha", chunk.getCommitSha());
            payload.put("content", chunk.getContent());

            Map<String, Object> point = Map.of(
                    "id", chunk.getId().toString(),
                    "vector", floatList,
                    "payload", payload
            );
            points.add(point);
        }

        Map<String, Object> request = Map.of("points", points);

        try {
            restClient.put()
                    .uri("/collections/{name}/points?wait=true", collectionName)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to upsert points to Qdrant: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upsert vector points to Qdrant: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteByRepositoryId(UUID repositoryId) {
        ensureCollectionExists();

        Map<String, Object> filter = Map.of(
                "filter", Map.of(
                        "must", List.of(
                                Map.of("key", "repositoryId", "match", Map.of("value", repositoryId.toString()))
                        )
                )
        );

        try {
            restClient.post()
                    .uri("/collections/{name}/points/delete?wait=true", collectionName)
                    .body(filter)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Deleted Qdrant vector points for repositoryId={}", repositoryId);
        } catch (Exception e) {
            log.warn("Failed to delete Qdrant points for repositoryId={}: {}", repositoryId, e.getMessage());
        }
    }

    @Override
    public List<SearchResultDto> search(UUID repositoryId, float[] queryVector, int topK) {
        ensureCollectionExists();

        List<Float> floatList = new ArrayList<>(queryVector.length);
        for (float f : queryVector) {
            floatList.add(f);
        }

        Map<String, Object> request = Map.of(
                "vector", floatList,
                "limit", Math.max(1, topK),
                "with_payload", true,
                "filter", Map.of(
                        "must", List.of(
                                Map.of("key", "repositoryId", "match", Map.of("value", repositoryId.toString()))
                        )
                )
        );

        try {
            QdrantSearchResponse response = restClient.post()
                    .uri("/collections/{name}/points/search", collectionName)
                    .body(request)
                    .retrieve()
                    .body(QdrantSearchResponse.class);

            if (response == null || response.result() == null) {
                return Collections.emptyList();
            }

            List<SearchResultDto> results = new ArrayList<>();
            for (ScoredPoint point : response.result()) {
                Map<String, Object> payload = point.payload();
                if (payload == null) {
                    continue;
                }

                UUID id = point.id() != null ? UUID.fromString(point.id()) : null;
                UUID repoId = payload.get("repositoryId") != null ? UUID.fromString(payload.get("repositoryId").toString()) : null;
                UUID fileId = payload.get("repositoryFileId") != null ? UUID.fromString(payload.get("repositoryFileId").toString()) : null;
                String filePath = (String) payload.get("filePath");
                String language = (String) payload.get("language");
                Integer startLine = payload.get("startLine") instanceof Number ? ((Number) payload.get("startLine")).intValue() : null;
                Integer endLine = payload.get("endLine") instanceof Number ? ((Number) payload.get("endLine")).intValue() : null;
                Integer chunkIndex = payload.get("chunkIndex") instanceof Number ? ((Number) payload.get("chunkIndex")).intValue() : null;
                String commitSha = (String) payload.get("commitSha");
                String content = (String) payload.get("content");

                results.add(SearchResultDto.builder()
                        .id(id)
                        .repositoryId(repoId)
                        .repositoryFileId(fileId)
                        .filePath(filePath)
                        .language(language)
                        .startLine(startLine)
                        .endLine(endLine)
                        .chunkIndex(chunkIndex)
                        .commitSha(commitSha)
                        .content(content)
                        .score(point.score())
                        .build());
            }

            return results;

        } catch (Exception e) {
            log.error("Failed to query Qdrant points for repositoryId={}: {}", repositoryId, e.getMessage(), e);
            throw new RuntimeException("Vector search failed: " + e.getMessage(), e);
        }
    }

    @Override
    public long countByRepositoryId(UUID repositoryId) {
        try {
            ensureCollectionExists();
            Map<String, Object> countRequest = Map.of(
                    "filter", Map.of(
                            "must", List.of(
                                    Map.of("key", "repositoryId", "match", Map.of("value", repositoryId.toString()))
                            )
                    ),
                    "exact", true
            );

            QdrantCountResponse response = restClient.post()
                    .uri("/collections/{name}/points/count", collectionName)
                    .body(countRequest)
                    .retrieve()
                    .body(QdrantCountResponse.class);

            if (response != null && response.result() != null && response.result().count() != null) {
                return response.result().count();
            }
        } catch (Exception e) {
            log.warn("Could not retrieve Qdrant count for repositoryId={}: {}", repositoryId, e.getMessage());
        }
        return 0L;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QdrantSearchResponse(
            @JsonProperty("result") List<ScoredPoint> result
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScoredPoint(
            @JsonProperty("id") String id,
            @JsonProperty("score") Double score,
            @JsonProperty("payload") Map<String, Object> payload
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QdrantCountResponse(
            @JsonProperty("result") CountResult result
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CountResult(
            @JsonProperty("count") Long count
    ) {}
}
