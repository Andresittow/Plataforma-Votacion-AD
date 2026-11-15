package com.votingplatform.voteprocessor.application;

import com.votingplatform.common.domain.*;
import com.votingplatform.voteprocessor.domain.*;
import com.votingplatform.voteprocessor.application.port.ResultsPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteProcessingServiceTest {
    @Mock ResultsPublisher publisher;
    @Captor ArgumentCaptor<TallySnapshot> snapshot;

    @Test
    void shouldPublishExactlyOnce_withCurrentTotalsAndFixedTime() {
        // Arrange
        var at = Instant.parse("2026-11-14T15:30:01Z");
        var service = new VoteProcessingService(new GlobalTally(), publisher, Clock.fixed(at, ZoneOffset.UTC));
        service.onVote(new VoteEvent(new VoterId("u-1"), new CandidateId("c-2"),
                new Region("norte"), new IpAddress("190.12.4.7"), at));
        // Act
        service.publishSnapshot();
        // Assert
        verify(publisher, times(1)).publishGlobal(snapshot.capture());
        verifyNoMoreInteractions(publisher);
        assertThat(snapshot.getValue().totals()).containsExactlyEntriesOf(Map.of("c-2", 1L));
        assertThat(snapshot.getValue().totalVotes()).isEqualTo(1);
        assertThat(snapshot.getValue().at()).isEqualTo(at);
    }
}