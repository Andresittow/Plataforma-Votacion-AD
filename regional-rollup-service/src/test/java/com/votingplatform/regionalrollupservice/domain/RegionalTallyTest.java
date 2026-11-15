package com.votingplatform.regionalrollupservice.domain;

import com.votingplatform.common.domain.*;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

class RegionalTallyTest {
    static final Instant AT = Instant.parse("2026-11-14T15:30:05Z");

    static VoteEvent vote(String user, String candidate, String region) {
        return new VoteEvent(new VoterId(user), new CandidateId(candidate), new Region(region),
                new IpAddress("190.12.4.7"), AT);
    }

    @Test
    void shouldGroupVotesByRegion_andWeightTotals() {
        // Arrange
        var tally = new RegionalTally();
        tally.registerVote(vote("u1", "c1", "norte"));
        tally.registerVote(vote("u2", "c1", "norte"));
        tally.registerVote(vote("u3", "c2", "sur"));
        // Act
        var snapshots = tally.snapshots(new PopulationWeighting(WeightingStrategyTest.weights()), AT);
        // Assert
        assertThat(snapshots).hasSize(2);
        var north = snapshots.stream().filter(s -> s.region().equals("norte")).findFirst().orElseThrow();
        assertThat(north.totals()).containsExactlyEntriesOf(Map.of("c1", 2L));
        assertThat(north.weightedTotals()).containsExactlyEntriesOf(Map.of("c1", 2.4));
        assertThat(north.strategy()).isEqualTo("PopulationWeighting");
        assertThat(north.at()).isEqualTo(AT);
    }

    @Test
    void shouldMoveVote_whenCandidateOrRegionChanges_andIgnoreRetries() {
        // Arrange
        var tally = new RegionalTally();
        tally.registerVote(vote("u1", "c1", "norte"));
        // Act
        tally.registerVote(vote("u1", "c2", "norte"));
        var afterCandidateChange = tally.snapshots(new UniformWeighting(), AT);
        tally.registerVote(vote("u1", "c2", "sur"));
        tally.registerVote(vote("u1", "c2", "sur"));
        var afterRegionChange = tally.snapshots(new UniformWeighting(), AT);
        tally.registerVote(vote("u1", "c3", "este"));
        var afterBothChange = tally.snapshots(new UniformWeighting(), AT);
        // Assert
        assertThat(afterCandidateChange.get(0).totals()).containsExactlyEntriesOf(Map.of("c2", 1L));
        assertThat(afterRegionChange).hasSize(1);
        assertThat(afterRegionChange.get(0).region()).isEqualTo("sur");
        assertThat(afterRegionChange.get(0).totals()).containsExactlyEntriesOf(Map.of("c2", 1L));
        assertThat(afterBothChange).hasSize(1);
        assertThat(afterBothChange.get(0).region()).isEqualTo("este");
        assertThat(afterBothChange.get(0).totals()).containsExactlyEntriesOf(Map.of("c3", 1L));
    }

    @Test
    void shouldReturnDefensiveImmutableSnapshots() {
        // Arrange
        var tally = new RegionalTally();
        tally.registerVote(vote("u1", "c1", "norte"));
        var totals = new HashMap<>(Map.of("c1", 1L));
        var weighted = new HashMap<>(Map.of("c1", 1.0));
        var copy = new RegionalSnapshot("norte", totals, weighted, "UniformWeighting", AT);
        // Act
        var snapshots = tally.snapshots(new UniformWeighting(), AT);
        totals.clear();
        weighted.clear();
        tally.registerVote(vote("u1", "c2", "sur"));
        // Assert
        assertThatThrownBy(() -> snapshots.clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshots.get(0).totals().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> snapshots.get(0).weightedTotals().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(copy.totals()).containsExactlyEntriesOf(Map.of("c1", 1L));
        assertThat(copy.weightedTotals()).containsExactlyEntriesOf(Map.of("c1", 1.0));
        assertThat(snapshots.get(0).region()).isEqualTo("norte");
    }

    @Test
    void shouldRejectNulls_andReturnNoSnapshotsWhenEmpty() {
        // Arrange
        var tally = new RegionalTally();
        // Act
        var snapshots = tally.snapshots(new UniformWeighting(), AT);
        // Assert
        assertThat(snapshots).isEmpty();
        assertThatNullPointerException().isThrownBy(() -> tally.registerVote(null));
        assertThatNullPointerException().isThrownBy(() -> tally.snapshots(null, AT));
        assertThatNullPointerException().isThrownBy(() -> tally.snapshots(new UniformWeighting(), null));
    }

    @Test
    void shouldKeepConsistentCounts_whenAccessIsConcurrent() throws Exception {
        // Arrange
        var tally = new RegionalTally();
        var pool = Executors.newFixedThreadPool(5);
        var start = new CountDownLatch(1);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int w = 0; w < 4; w++) {
            final int worker = w;
            tasks.add(() -> {
                start.await();
                for (int i = 0; i < 50; i++) {
                    tally.registerVote(vote("u" + worker + "-" + i, "c1", "norte"));
                    tally.registerVote(vote("u" + worker + "-" + i, "c2", "sur"));
                }
                return null;
            });
        }
        tasks.add(() -> {
            start.await();
            for (int i = 0; i < 100; i++) {
                for (var snapshot : tally.snapshots(new UniformWeighting(), AT)) {
                    assertThat(snapshot.totals().values()).allMatch(count -> count > 0);
                    snapshot.totals().forEach((candidate, count) ->
                            assertThat(snapshot.weightedTotals().get(candidate)).isEqualTo(count.doubleValue()));
                }
            }
            return null;
        });
        try {
            // Act
            var futures = tasks.stream().map(pool::submit).toList();
            start.countDown();
            for (var future : futures) future.get(5, TimeUnit.SECONDS);
            // Assert
            var snapshots = tally.snapshots(new UniformWeighting(), AT);
            assertThat(snapshots).hasSize(1);
            assertThat(snapshots.get(0).region()).isEqualTo("sur");
            assertThat(snapshots.get(0).totals()).containsExactlyEntriesOf(Map.of("c2", 200L));
        } finally {
            pool.shutdownNow();
        }
    }
}