package com.example.coderag.vector;

import com.example.coderag.dto.SearchResultDto;
import com.example.coderag.model.CodeChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QdrantVectorStoreServiceTest {

    private MockRestServiceServer mockServer;
    private QdrantVectorStoreService vectorStoreService;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("http://localhost:6333");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();

        vectorStoreService = new QdrantVectorStoreService("code_chunks", restClient);
    }

    @Test
    void search_ShouldReturnRankedResults() {
        UUID repoId = UUID.randomUUID();
        UUID chunkId = UUID.randomUUID();

        // 1. Mock collection check
        mockServer.expect(requestTo("http://localhost:6333/collections/code_chunks"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        // 2. Mock search call
        String searchResponseJson = """
                {
                  "result": [
                    {
                      "id": "%s",
                      "score": 0.925,
                      "payload": {
                        "repositoryId": "%s",
                        "filePath": "src/AuthService.java",
                        "startLine": 1,
                        "endLine": 120,
                        "chunkIndex": 0,
                        "content": "public class AuthService { ... }"
                      }
                    }
                  ]
                }
                """.formatted(chunkId, repoId);

        mockServer.expect(requestTo("http://localhost:6333/collections/code_chunks/points/search"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(searchResponseJson, MediaType.APPLICATION_JSON));

        List<SearchResultDto> results = vectorStoreService.search(repoId, new float[]{0.1f, 0.2f}, 10);

        assertNotNull(results);
        assertEquals(1, results.size());
        SearchResultDto first = results.get(0);
        assertEquals(chunkId, first.getId());
        assertEquals("src/AuthService.java", first.getFilePath());
        assertEquals(0.925, first.getScore(), 0.001);
        assertEquals(1, first.getStartLine());
        assertEquals(120, first.getEndLine());
        mockServer.verify();
    }

    @Test
    void countByRepositoryId_ShouldReturnCount() {
        UUID repoId = UUID.randomUUID();

        // 1. Mock collection check
        mockServer.expect(requestTo("http://localhost:6333/collections/code_chunks"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        // 2. Mock count call
        String countResponseJson = """
                {
                  "result": {
                    "count": 42
                  }
                }
                """;

        mockServer.expect(requestTo("http://localhost:6333/collections/code_chunks/points/count"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(countResponseJson, MediaType.APPLICATION_JSON));

        long count = vectorStoreService.countByRepositoryId(repoId);
        assertEquals(42L, count);
        mockServer.verify();
    }
}
