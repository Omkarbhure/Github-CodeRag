package com.example.coderag.chat;

import com.example.coderag.dto.SearchResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContextBuilderTest {

    private ContextBuilder contextBuilder;

    @BeforeEach
    void setUp() {
        contextBuilder = new ContextBuilder();
    }

    @Test
    void buildContext_ShouldReturnEmptyString_WhenResultsEmpty() {
        String result = contextBuilder.buildContext(Collections.emptyList());
        assertEquals("", result);
    }

    @Test
    void buildContext_ShouldIncludeFileAndLineHeaders() {
        SearchResultDto chunk1 = SearchResultDto.builder()
                .filePath("src/AuthService.java")
                .startLine(1)
                .endLine(30)
                .content("public class AuthService { void login() {} }")
                .build();

        SearchResultDto chunk2 = SearchResultDto.builder()
                .filePath("src/JwtService.java")
                .startLine(10)
                .endLine(45)
                .content("public class JwtService { String generateToken() {} }")
                .build();

        String context = contextBuilder.buildContext(List.of(chunk1, chunk2));

        assertNotNull(context);
        assertTrue(context.contains("--- File: src/AuthService.java (Lines 1-30) ---"));
        assertTrue(context.contains("public class AuthService { void login() {} }"));
        assertTrue(context.contains("--- File: src/JwtService.java (Lines 10-45) ---"));
        assertTrue(context.contains("public class JwtService { String generateToken() {} }"));
    }

    @Test
    void buildContext_ShouldRespectMaxChunksLimit() {
        SearchResultDto chunk1 = SearchResultDto.builder().filePath("A.java").startLine(1).endLine(10).content("class A").build();
        SearchResultDto chunk2 = SearchResultDto.builder().filePath("B.java").startLine(1).endLine(10).content("class B").build();
        SearchResultDto chunk3 = SearchResultDto.builder().filePath("C.java").startLine(1).endLine(10).content("class C").build();

        String context = contextBuilder.buildContext(List.of(chunk1, chunk2, chunk3), 2, 5000);

        assertTrue(context.contains("A.java"));
        assertTrue(context.contains("B.java"));
        assertTrue(!context.contains("C.java"));
    }
}
