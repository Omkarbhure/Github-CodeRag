package com.example.coderag.service;

import com.example.coderag.model.IndexingJob;
import com.example.coderag.model.IndexingStatus;
import com.example.coderag.repository.IndexingJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class StartupOrphanJobCleaner {

    private static final Logger log = LoggerFactory.getLogger(StartupOrphanJobCleaner.class);

    private final IndexingJobRepository indexingJobRepository;

    public StartupOrphanJobCleaner(IndexingJobRepository indexingJobRepository) {
        this.indexingJobRepository = indexingJobRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void cleanupOrphanedJobs() {
        List<IndexingStatus> activeStatuses = List.of(
                IndexingStatus.PENDING,
                IndexingStatus.DOWNLOADING,
                IndexingStatus.SCANNING,
                IndexingStatus.CHUNKING,
                IndexingStatus.EMBEDDING
        );

        List<IndexingJob> orphanedJobs = indexingJobRepository.findByStatusIn(activeStatuses);
        if (orphanedJobs.isEmpty()) {
            log.info("Startup orphan check: No orphaned indexing jobs found.");
            return;
        }

        log.warn("Startup orphan check: Found {} non-terminal indexing job(s) from prior run. Marking as FAILED.", orphanedJobs.size());
        for (IndexingJob job : orphanedJobs) {
            job.setStatus(IndexingStatus.FAILED);
            job.setErrorMessage("Interrupted by server restart — please re-import");
            job.setCompletedAt(OffsetDateTime.now());
            indexingJobRepository.save(job);
            log.info("Marked orphaned job [{}] (repo: {}) as FAILED", job.getId(), job.getRepositoryId());
        }
    }
}
