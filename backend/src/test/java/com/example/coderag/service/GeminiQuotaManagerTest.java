package com.example.coderag.service;

import com.example.coderag.exception.GeminiQuotaExceededException;
import com.example.coderag.ratelimit.GeminiQuotaManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeminiQuotaManagerTest {

    @Test
    void acquireQuota_ShouldAllowCallsWithinDailyLimit() {
        GeminiQuotaManager quotaManager = new GeminiQuotaManager(3, true);

        assertDoesNotThrow(quotaManager::acquireQuota);
        assertDoesNotThrow(quotaManager::acquireQuota);
        assertDoesNotThrow(quotaManager::acquireQuota);

        assertEquals(3, quotaManager.getUsageToday());
    }

    @Test
    void acquireQuota_ShouldThrowException_WhenDailyLimitExceeded() {
        GeminiQuotaManager quotaManager = new GeminiQuotaManager(2, true);

        quotaManager.acquireQuota();
        quotaManager.acquireQuota();

        assertThrows(GeminiQuotaExceededException.class, quotaManager::acquireQuota);
    }

    @Test
    void acquireQuota_ShouldBypass_WhenQuotaDisabled() {
        GeminiQuotaManager quotaManager = new GeminiQuotaManager(1, false);

        assertDoesNotThrow(quotaManager::acquireQuota);
        assertDoesNotThrow(quotaManager::acquireQuota);
        assertDoesNotThrow(quotaManager::acquireQuota);
    }

    @Test
    void reset_ShouldClearCount() {
        GeminiQuotaManager quotaManager = new GeminiQuotaManager(2, true);
        quotaManager.acquireQuota();
        quotaManager.acquireQuota();

        quotaManager.reset();
        assertEquals(0, quotaManager.getUsageToday());
        assertDoesNotThrow(quotaManager::acquireQuota);
    }
}
