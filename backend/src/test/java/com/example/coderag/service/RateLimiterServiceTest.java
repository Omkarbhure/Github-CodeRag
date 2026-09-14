package com.example.coderag.service;

import com.example.coderag.exception.RateLimitException;
import com.example.coderag.ratelimit.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        // Limits: 2 imports/hr, 5 chats/hr, 3 intelligence/hr
        rateLimiterService = new RateLimiterService(2, 5, 3);
    }

    @Test
    void checkRateLimit_ShouldAllowRequestsWithinLimit() {
        UUID userId = UUID.randomUUID();

        assertDoesNotThrow(() -> rateLimiterService.checkRateLimit(userId, RateLimiterService.ActionType.IMPORT));
        assertDoesNotThrow(() -> rateLimiterService.checkRateLimit(userId, RateLimiterService.ActionType.IMPORT));
    }

    @Test
    void checkRateLimit_ShouldThrowRateLimitException_WhenExceedingLimit() {
        UUID userId = UUID.randomUUID();

        rateLimiterService.checkRateLimit(userId, RateLimiterService.ActionType.IMPORT);
        rateLimiterService.checkRateLimit(userId, RateLimiterService.ActionType.IMPORT);

        RateLimitException ex = assertThrows(RateLimitException.class, () ->
                rateLimiterService.checkRateLimit(userId, RateLimiterService.ActionType.IMPORT)
        );

        assertEquals("IMPORT", ex.getAction());
        assertTrue(ex.getRetryAfterSeconds() > 0 && ex.getRetryAfterSeconds() <= 3600);
    }

    @Test
    void checkRateLimit_ShouldTrackActionsIndependently() {
        UUID userId = UUID.randomUUID();

        // Max out imports (2)
        rateLimiterService.checkRateLimit(userId, RateLimiterService.ActionType.IMPORT);
        rateLimiterService.checkRateLimit(userId, RateLimiterService.ActionType.IMPORT);

        // Chat should still be allowed
        assertDoesNotThrow(() -> rateLimiterService.checkRateLimit(userId, RateLimiterService.ActionType.CHAT));
    }

    @Test
    void checkRateLimit_ShouldTrackUsersIndependently() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        // Max out user1
        rateLimiterService.checkRateLimit(user1, RateLimiterService.ActionType.IMPORT);
        rateLimiterService.checkRateLimit(user1, RateLimiterService.ActionType.IMPORT);

        assertThrows(RateLimitException.class, () ->
                rateLimiterService.checkRateLimit(user1, RateLimiterService.ActionType.IMPORT)
        );

        // user2 should still be allowed
        assertDoesNotThrow(() -> rateLimiterService.checkRateLimit(user2, RateLimiterService.ActionType.IMPORT));
    }
}
