package com.votingplatform.voteprocessor.domain;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record TallySnapshot(Map<String, Long> totals, long totalVotes, Instant at) {
    public TallySnapshot {
        totals = Map.copyOf(Objects.requireNonNull(totals, "totals"));
        Objects.requireNonNull(at, "at");
        if (totalVotes < 0) throw new IllegalArgumentException("totalVotes must be nonnegative");
        long sum = 0;
        for (var entry : totals.entrySet()) {
            if (entry.getKey().isBlank() || entry.getValue() <= 0) {
                throw new IllegalArgumentException("Totals require nonblank candidates and positive counts");
            }
            sum = Math.addExact(sum, entry.getValue());
        }
        if (sum != totalVotes) throw new IllegalArgumentException("totalVotes must equal sum of totals");
    }
}