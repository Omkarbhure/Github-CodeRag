package com.example.coderag.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiAnswerServiceTest {

    private MockRestServiceServer mockServer;
    private GeminiAnswerService answerService;

    @BeforeEach
    void setUp() {
        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com");
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();

        answerService = new GeminiAnswerService(
                "test-api-key",
                "gemini-1.5-flash",
                restClient
        );
    }

    @Test
    void generateAnswer_ShouldReturnAnswerWithCitations() {
        String responseJson = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "Authentication is handled in AuthService [src/AuthService.java:10-25]."
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        String answer = answerService.generateAnswer(
                "How does login work?",
                "--- File: src/AuthService.java (Lines 10-25) ---\npublic void login() {}",
                Collections.emptyList()
        );

        assertNotNull(answer);
        assertEquals("Authentication is handled in AuthService [src/AuthService.java:10-25].", answer);
        mockServer.verify();
    }

    @Test
    void generateAnswer_ShouldFailFast_WhenAuthError401() {
        mockServer.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=test-api-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("{\"error\": \"API key invalid\"}"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                answerService.generateAnswer("Question", "Context", Collections.emptyList())
        );

        assertTrue(ex.getMessage().contains("GEMINI_API_KEY"));
        mockServer.verify();
    }
}
