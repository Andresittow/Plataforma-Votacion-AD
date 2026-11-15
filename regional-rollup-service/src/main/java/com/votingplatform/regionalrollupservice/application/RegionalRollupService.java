package com.votingplatform.regionalrollupservice.application;
import com.votingplatform.common.domain.VoteEvent;
import com.votingplatform.regionalrollupservice.domain.*;
import com.votingplatform.regionalrollupservice.application.port.RegionalResultsPublisher;
import java.time.Clock;
import java.util.Objects;

public final class RegionalRollupService {
    private final RegionalTally tally;
    private final WeightingStrategy strategy;
    private final RegionalResultsPublisher publisher;
    private final Clock clock;

    public RegionalRollupService(RegionalTally tally, WeightingStrategy strategy,
            RegionalResultsPublisher publisher, Clock clock) {
        this.tally = Objects.requireNonNull(tally, "tally");
        this.strategy = Objects.requireNonNull(strategy, "strategy");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void onVote(VoteEvent vote) { tally.registerVote(vote); }

    public void publishSnapshots() {
        tally.snapshots(strategy, clock.instant()).forEach(publisher::publishRegional);
    }
}