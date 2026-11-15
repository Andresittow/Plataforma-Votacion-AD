package com.votingplatform.voteprocessor.infrastructure;

import com.votingplatform.voteprocessor.application.VoteProcessingService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class SnapshotScheduler {
    private final VoteProcessingService service;

    public SnapshotScheduler(VoteProcessingService service) {
        this.service = service;
    }

    @Scheduled(fixedRate = 1000)
    public void publishSnapshot() {
        service.publishSnapshot();
    }
}