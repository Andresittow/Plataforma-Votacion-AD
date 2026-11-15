package com.votingplatform.voteprocessor.application;

import com.votingplatform.common.domain.VoteEvent;
import com.votingplatform.voteprocessor.application.port.ResultsPublisher;
import com.votingplatform.voteprocessor.domain.GlobalTally;
import java.time.Clock;
import java.util.Objects;

public final class VoteProcessingService {
    private final GlobalTally tally;
    private final ResultsPublisher publisher;
    private final Clock clock;

    public VoteProcessingService(GlobalTally tally, ResultsPublisher publisher, Clock clock) {
        this.tally = Objects.requireNonNull(tally, "tally");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void onVote(VoteEvent vote) {
        tally.registerVote(vote);
    }

    public void publishSnapshot() {
        publisher.publishGlobal(tally.snapshot(clock.instant()));
    }
}