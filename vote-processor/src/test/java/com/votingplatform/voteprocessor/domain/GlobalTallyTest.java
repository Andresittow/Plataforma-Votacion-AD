package com.votingplatform.voteprocessor.domain;

import com.votingplatform.common.domain.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class GlobalTallyTest {
    private static final Instant AT = Instant.parse("2026-11-14T15:30:00Z");

    private VoteEvent vote(String user, String candidate) {
        return new VoteEvent(new VoterId(user), new CandidateId(candidate),
                new Region("norte"), new IpAddress("190.12.4.7"), AT);
    }

    @Test
    void shouldCountVotes_whenVotersAreDistinct() {
        // Arrange
        var tally = new GlobalTally();
        // Act
        tally.registerVote(vote("u-1", "c-1"));
        tally.registerVote(vote("u-2", "c-1"));
        tally.registerVote(vote("u-3", "c-2"));
        var snapshot = tally.snapshot(AT);
        // Assert
        assertThat(snapshot.totals()).containsExactlyInAnyOrderEntriesOf(Map.of("c-1", 2L, "c-2", 1L));
        assertThat(snapshot.totalVotes()).isEqualTo(3);
        assertThat(snapshot.at()).isEqualTo(AT);
    }

    @Test
    void shouldMoveVote_whenVoterChangesCandidate() {
        // Arrange
        var tally = new GlobalTally();
        tally.registerVote(vote("u-1", "c-1"));
        // Act
        tally.registerVote(vote("u-1", "c-2"));
        // Assert
        assertThat(tally.snapshot(AT).totals()).containsExactlyEntriesOf(Map.of("c-2", 1L));
        assertThat(tally.snapshot(AT).totalVotes()).isEqualTo(1);
    }

    @Test
    void shouldKeepCount_whenVoteIsRetried() {
        // Arrange
        var tally = new GlobalTally();
        var event = vote("u-1", "c-1");
        tally.registerVote(event);
        // Act
        tally.registerVote(event);
        // Assert
        assertThat(tally.snapshot(AT).totals()).containsExactlyEntriesOf(Map.of("c-1", 1L));
        assertThat(tally.snapshot(AT).totalVotes()).isEqualTo(1);
    }

    @Test
    void shouldProtectSnapshot_whenMapsAreModified() {
        // Arrange
        var tally = new GlobalTally();
        tally.registerVote(vote("u-1", "c-1"));
        var snapshot = tally.snapshot(AT);
        var source = new HashMap<>(Map.of("c-1", 1L));
        var copied = new TallySnapshot(source, 1, AT);
        // Act
        source.clear();
        tally.registerVote(vote("u-1", "c-2"));
        // Assert
        assertThatThrownBy(() -> snapshot.totals().put("c-3", 9L))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(snapshot.totals()).containsExactlyEntriesOf(Map.of("c-1", 1L));
        assertThat(copied.totals()).containsExactlyEntriesOf(Map.of("c-1", 1L));
        assertThat(tally.snapshot(AT).totals()).containsExactlyEntriesOf(Map.of("c-2", 1L));
    }

    @Test
    void shouldRejectNulls_andStartEmpty() {
        // Arrange
        var tally = new GlobalTally();
        // Act
        var snapshot = tally.snapshot(AT);
        // Assert
        assertThat(snapshot.totals()).isEmpty();
        assertThat(snapshot.totalVotes()).isZero();
        assertThatNullPointerException().isThrownBy(() -> tally.registerVote(null));
        assertThatNullPointerException().isThrownBy(() -> tally.snapshot(null));
        assertThatNullPointerException().isThrownBy(() -> new TallySnapshot(null, 0, AT));
        assertThatNullPointerException().isThrownBy(() -> new TallySnapshot(Map.of(), 0, null));
        assertThatIllegalArgumentException().isThrownBy(() -> new TallySnapshot(Map.of(), -1, AT));
    }

    @Test
    void shouldRemainConsistent_whenVotesAndSnapshotsAreConcurrent() throws Exception {
        // Arrange
        var tally = new GlobalTally();
        var executor = Executors.newFixedThreadPool(5);
        var start = new CountDownLatch(1);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int worker = 0; worker < 4; worker++) {
            final int id = worker;
            tasks.add(() -> {
                start.await();
                for (int user = 0; user < 100; user++) {
                    tally.registerVote(vote("u-" + id + "-" + user, "c-1"));
                    tally.registerVote(vote("u-" + id + "-" + user, "c-2"));
                }
                return null;
            });
        }
        tasks.add(() -> {
            start.await();
            for (int i = 0; i < 400; i++) {
                var snapshot = tally.snapshot(AT);
                assertThat(snapshot.totals().values().stream().mapToLong(Long::longValue).sum())
                        .isEqualTo(snapshot.totalVotes());
                assertThat(snapshot.totals().values()).allMatch(total -> total > 0);
            }
            return null;
        });
        try {
            // Act
            var futures = tasks.stream().map(executor::submit).toList();
            start.countDown();
            for (var future : futures) future.get(5, TimeUnit.SECONDS);
            // Assert
            assertThat(tally.snapshot(AT).totalVotes()).isEqualTo(400);
            assertThat(tally.snapshot(AT).totals()).containsExactlyEntriesOf(Map.of("c-2", 400L));
        } finally {
            executor.shutdownNow();
        }
    }
}