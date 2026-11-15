package com.votingplatform.voteprocessor.domain;

import com.votingplatform.common.domain.CandidateId;
import com.votingplatform.common.domain.VoteEvent;
import com.votingplatform.common.domain.VoterId;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class GlobalTally {
    private final Map<VoterId, CandidateId> lastVotes = new HashMap<>();
    private final Map<CandidateId, Long> totals = new HashMap<>();

    public synchronized void registerVote(VoteEvent vote) {
        Objects.requireNonNull(vote, "vote");
        CandidateId candidate = vote.candidateId();
        CandidateId previous = lastVotes.get(vote.userId());
        if (candidate.equals(previous)) return;
        if (previous != null) {
            totals.computeIfPresent(previous, (id, count) -> count == 1 ? null : count - 1);
        }
        lastVotes.put(vote.userId(), candidate);
        totals.merge(candidate, 1L, Long::sum);
    }

    public synchronized TallySnapshot snapshot(Instant at) {
        Objects.requireNonNull(at, "at");
        Map<String, Long> snapshotTotals = new HashMap<>();
        totals.forEach((candidate, count) -> {
            if (count > 0) snapshotTotals.put(candidate.value(), count);
        });
        return new TallySnapshot(snapshotTotals, lastVotes.size(), at);
    }
}