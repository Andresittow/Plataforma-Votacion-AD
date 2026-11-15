package com.votingplatform.regionalrollupservice.infrastructure;
import com.votingplatform.regionalrollupservice.application.RegionalRollupService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public final class SnapshotScheduler {
    private final RegionalRollupService service;
    public SnapshotScheduler(RegionalRollupService service) { this.service = service; }

    @Scheduled(fixedRate = 5000)
    public void publishSnapshots() { service.publishSnapshots(); }
}