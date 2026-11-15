package com.votingplatform.common.domain;

import java.util.Objects;

public record VoterRecord(VoterId userId, Region region, boolean active) {
    public VoterRecord {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(region, "region must not be null");
    }
}
