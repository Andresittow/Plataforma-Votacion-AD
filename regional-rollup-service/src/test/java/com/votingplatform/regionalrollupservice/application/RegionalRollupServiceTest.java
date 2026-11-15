package com.votingplatform.regionalrollupservice.application;

import com.votingplatform.common.domain.*;
import com.votingplatform.regionalrollupservice.domain.*;
import com.votingplatform.regionalrollupservice.application.port.RegionalResultsPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegionalRollupServiceTest {
    @Mock RegionalResultsPublisher publisher;
    @Captor ArgumentCaptor<RegionalSnapshot> captor;
    private static final Instant AT = Instant.parse("2026-11-14T15:30:05Z");

    static Stream<WeightingStrategy> strategies() {
        return Stream.of(new UniformWeighting(), new PopulationWeighting(Map.of(
                new Region("norte"), 1.2, new Region("sur"), .9, new Region("este"), 1.0,
                new Region("oeste"), 1.1, new Region("centro"), 1.0)));
    }

    private VoteEvent vote(String user, String region) {
        return new VoteEvent(new VoterId(user), new CandidateId("c1"), new Region(region),
                new IpAddress("190.12.4.7"), AT);
    }

    @ParameterizedTest
    @MethodSource("strategies")
    void shouldPublishOncePerPopulatedRegion_withEitherStrategy(WeightingStrategy strategy) {
        // Arrange
        var service = new RegionalRollupService(new RegionalTally(), strategy, publisher, Clock.fixed(AT, ZoneOffset.UTC));
        service.onVote(vote("u1", "norte"));
        service.onVote(vote("u2", "sur"));
        service.onVote(vote("u3", "norte"));
        // Act
        service.publishSnapshots();
        // Assert
        verify(publisher, times(2)).publishRegional(captor.capture());
        verifyNoMoreInteractions(publisher);
        assertThat(captor.getAllValues()).extracting(RegionalSnapshot::region).containsExactlyInAnyOrder("norte", "sur");
        for (var snapshot : captor.getAllValues()) {
            assertThat(snapshot.at()).isEqualTo(AT);
            assertThat(snapshot.strategy()).isEqualTo(strategy.name());
            snapshot.totals().forEach((candidate, count) ->
                    assertThat(snapshot.weightedTotals().get(candidate))
                            .isEqualTo(count * strategy.weightFor(new Region(snapshot.region()))));
        }
        assertThat(captor.getAllValues().stream().flatMap(s -> s.totals().values().stream()).mapToLong(Long::longValue).sum()).isEqualTo(3);
    }

    @Test
    void shouldNotPublish_whenThereAreNoVotes() {
        // Arrange
        var service = new RegionalRollupService(new RegionalTally(), new UniformWeighting(), publisher, Clock.fixed(AT, ZoneOffset.UTC));
        // Act
        service.publishSnapshots();
        // Assert
        verifyNoInteractions(publisher);
    }
}