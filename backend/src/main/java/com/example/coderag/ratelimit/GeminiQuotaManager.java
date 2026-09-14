package com.example.coderag.ratelimit;

import com.example.coderag.exception.GeminiQuotaExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class GeminiQuotaManager {

    private static final Logger log = LoggerFactory.getLogger(GeminiQuotaManager.class);

    private final long dailyQuota;
    private final boolean quotaEnabled;

    private volatile LocalDate currentDate;
    private final AtomicLong callCounter = new AtomicLong(0);

    public GeminiQuotaManager(
            @Value("${app.gemini.daily-quota:1500}") long dailyQuota,
            @Value("${app.gemini.quota-enabled:true}") boolean quotaEnabled
    ) {
        this.dailyQuota = dailyQuota;
        this.quotaEnabled = quotaEnabled;
        this.currentDate = LocalDate.now(ZoneOffset.UTC);
    }

    public synchronized void acquireQuota() {
        if (!quotaEnabled) {
            return;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (!today.equals(currentDate)) {
            log.info("Resetting daily Gemini quota counter for new date: {}", today);
            currentDate = today;
            callCounter.set(0);
        }

        long currentCount = callCounter.get();
        if (currentCount >= dailyQuota) {
            log.warn("Gemini global daily quota reached: {} / {} calls. Rejecting request.", currentCount, dailyQuota);
            throw new GeminiQuotaExceededException(dailyQuota);
        }

        long updated = callCounter.incrementAndGet();
        if (updated % 100 == 0 || updated >= dailyQuota - 50) {
            log.info("Gemini API daily usage: {}/{} calls", updated, dailyQuota);
        }
    }

    public long getUsageToday() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (!today.equals(currentDate)) {
            return 0;
        }
        return callCounter.get();
    }

    public long getDailyQuota() {
        return dailyQuota;
    }

    public boolean isQuotaEnabled() {
        return quotaEnabled;
    }

    public synchronized void reset() {
        callCounter.set(0);
        currentDate = LocalDate.now(ZoneOffset.UTC);
    }
}
