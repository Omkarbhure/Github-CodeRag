package com.example.coderag.ratelimit;

import com.example.coderag.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);
    private static final long ONE_HOUR_MS = 3600_000L;

    public enum ActionType {
        IMPORT,
        CHAT,
        INTELLIGENCE
    }

    private final int importPerHour;
    private final int chatPerHour;
    private final int intelligencePerHour;

    private final Map<String, Deque<Long>> requestHistory = new ConcurrentHashMap<>();

    public RateLimiterService(
            @Value("${app.rate-limit.import-per-hour:5}") int importPerHour,
            @Value("${app.rate-limit.chat-per-hour:30}") int chatPerHour,
            @Value("${app.rate-limit.intelligence-per-hour:15}") int intelligencePerHour
    ) {
        this.importPerHour = importPerHour;
        this.chatPerHour = chatPerHour;
        this.intelligencePerHour = intelligencePerHour;
    }

    public void checkRateLimit(UUID userId, ActionType action) {
        if (userId == null) {
            return; // Skip rate limiting for unauthenticated or system calls
        }

        int maxAllowed = getMaxAllowedForAction(action);
        String key = action.name() + ":" + userId;
        long now = Instant.now().toEpochMilli();
        long windowStart = now - ONE_HOUR_MS;

        Deque<Long> timestamps = requestHistory.computeIfAbsent(key, k -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Evict expired entries older than 1 hour
            while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
                timestamps.pollFirst();
            }

            if (timestamps.size() >= maxAllowed) {
                long oldest = timestamps.peekFirst() != null ? timestamps.peekFirst() : windowStart;
                long retryAfterSeconds = Math.max(1, ((oldest + ONE_HOUR_MS) - now + 999) / 1000);
                log.warn("Rate limit exceeded for user {} on action {} (limit: {}/hr). Retry in {}s",
                        userId, action, maxAllowed, retryAfterSeconds);
                throw new RateLimitException(action.name(), retryAfterSeconds);
            }

            timestamps.addLast(now);
        }
    }

    private int getMaxAllowedForAction(ActionType action) {
        return switch (action) {
            case IMPORT -> importPerHour;
            case CHAT -> chatPerHour;
            case INTELLIGENCE -> intelligencePerHour;
        };
    }

    // Visible for testing / cleanup
    public void reset() {
        requestHistory.clear();
    }
}
