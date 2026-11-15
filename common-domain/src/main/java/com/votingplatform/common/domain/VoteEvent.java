package com.votingplatform.common.domain;

import java.time.Instant;
import java.util.Objects;

public record VoteEvent(VoterId userId, CandidateId candidateId, Region region,
                        IpAddress ip, Instant timestamp) {
    public VoteEvent {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(candidateId, "candidateId must not be null");
        Objects.requireNonNull(region, "region must not be null");
        Objects.requireNonNull(ip, "ip must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
