package com.example.coderag.vector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiEmbeddingServiceTest {

    private MockRestServiceServer mockServer;
    private GeminiEmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();

        embeddingService = new GeminiEmbeddingService(
                "test-api-key",
                "text-embedding-004",
                "https://generativelanguage.googleapis.com",
                restClient
        );
    }

    @Test
    void embed_ShouldReturnEmbeddingVector_WhenApiSucceeds() {
        String responseJson = """
                {
                  "embedding": {
                    "values": [0.12, -0.34, 0.56, 0.78]
                  }
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=test-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        float[] vector = embeddingService.embed("function hello() { return 'world'; }");

        assertNotNull(vector);
        assertEquals(4, vector.length);
        assertEquals(0.12f, vector[0], 0.001f);
        assertEquals(-0.34f, vector[1], 0.001f);
        mockServer.verify();
    }

    @Test
    void embed_ShouldFailFast_WhenAuthFailsWith401() {
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=test-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"error\": \"API key invalid\"}"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                embeddingService.embed("some code")
        );

        assertTrue(ex.getMessage().contains("GEMINI_API_KEY"));
        mockServer.verify();
    }

    @Test
    void embed_ShouldFailFast_WhenApiKeyNotConfigured() {
        GeminiEmbeddingService unconfiguredService = new GeminiEmbeddingService(
                "",
                "text-embedding-004",
                "https://generativelanguage.googleapis.com"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                unconfiguredService.embed("code")
        );
        assertTrue(ex.getMessage().contains("GEMINI_API_KEY is not configured"));
    }
}
