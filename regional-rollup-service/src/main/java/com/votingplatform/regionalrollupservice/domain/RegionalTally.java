package com.votingplatform.regionalrollupservice.domain;
import com.votingplatform.common.domain.*;
import java.time.Instant;
import java.util.*;

public final class RegionalTally {
    private final Map<VoterId, VoteEvent> lastVotes = new HashMap<>();
    private final Map<Region, Map<CandidateId, Long>> totals = new HashMap<>();

    public synchronized void registerVote(VoteEvent vote) {
        Objects.requireNonNull(vote, "vote");
        var previous = lastVotes.get(vote.userId());
        if (previous != null) {
            var previousTotals = totals.get(previous.region());
            previousTotals.computeIfPresent(previous.candidateId(),
                    (candidate, count) -> count == 1 ? null : count - 1);
            if (previousTotals.isEmpty()) totals.remove(previous.region());
        }
        totals.computeIfAbsent(vote.region(), region -> new HashMap<>())
                .merge(vote.candidateId(), 1L, Long::sum);
        lastVotes.put(vote.userId(), vote);
    }

    public synchronized List<RegionalSnapshot> snapshots(WeightingStrategy strategy, Instant at) {
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(at, "at");
        List<RegionalSnapshot> snapshots = new ArrayList<>();
        totals.forEach((region, counts) -> {
            Map<String, Long> raw = new HashMap<>();
            Map<String, Double> weighted = new HashMap<>();
            double weight = strategy.weightFor(region);
            counts.forEach((candidate, count) -> {
                raw.put(candidate.value(), count);
                weighted.put(candidate.value(), count * weight);
            });
            snapshots.add(new RegionalSnapshot(region.value(), raw, weighted, strategy.name(), at));
        });
        return List.copyOf(snapshots);
    }
}