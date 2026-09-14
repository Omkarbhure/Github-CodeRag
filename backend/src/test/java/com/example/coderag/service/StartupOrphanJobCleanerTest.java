package com.example.coderag.service;

import com.example.coderag.model.IndexingJob;
import com.example.coderag.model.IndexingStatus;
import com.example.coderag.repository.IndexingJobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StartupOrphanJobCleanerTest {

    @Mock
    private IndexingJobRepository indexingJobRepository;

    @InjectMocks
    private StartupOrphanJobCleaner orphanJobCleaner;

    @Test
    void cleanupOrphanedJobs_ShouldMarkNonTerminalJobsAsFailed() {
        IndexingJob job1 = IndexingJob.builder()
                .id(UUID.randomUUID())
                .repositoryId(UUID.randomUUID())
                .status(IndexingStatus.DOWNLOADING)
                .startedAt(OffsetDateTime.now().minusMinutes(10))
                .build();

        IndexingJob job2 = IndexingJob.builder()
                .id(UUID.randomUUID())
                .repositoryId(UUID.randomUUID())
                .status(IndexingStatus.EMBEDDING)
                .startedAt(OffsetDateTime.now().minusMinutes(5))
                .build();

        when(indexingJobRepository.findByStatusIn(anyList())).thenReturn(List.of(job1, job2));

        orphanJobCleaner.cleanupOrphanedJobs();

        assertEquals(IndexingStatus.FAILED, job1.getStatus());
        assertEquals("Interrupted by server restart — please re-import", job1.getErrorMessage());
        assertNotNull(job1.getCompletedAt());

        assertEquals(IndexingStatus.FAILED, job2.getStatus());
        assertEquals("Interrupted by server restart — please re-import", job2.getErrorMessage());
        assertNotNull(job2.getCompletedAt());

        verify(indexingJobRepository, times(2)).save(any(IndexingJob.class));
    }

    @Test
    void cleanupOrphanedJobs_ShouldDoNothing_WhenNoActiveJobsFound() {
        when(indexingJobRepository.findByStatusIn(anyList())).thenReturn(List.of());

        orphanJobCleaner.cleanupOrphanedJobs();

        verify(indexingJobRepository, never()).save(any(IndexingJob.class));
    }
}
