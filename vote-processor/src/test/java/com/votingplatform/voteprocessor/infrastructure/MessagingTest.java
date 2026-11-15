package com.votingplatform.voteprocessor.infrastructure;

import com.votingplatform.common.domain.VoteEvent;
import com.votingplatform.voteprocessor.application.VoteProcessingService;
import com.votingplatform.voteprocessor.domain.TallySnapshot;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingTest {
    @Mock VoteProcessingService service;
    @Mock RabbitTemplate rabbit;
    @Captor ArgumentCaptor<VoteEvent> vote;

    @Test
    void shouldDeserializeFlatContract_whenVoteIsValid() {
        // Arrange
        var listener = new RawVotesKafkaListener(new VoteProcessorConfiguration().objectMapper(), service);
        // Act
        listener.onMessage("""
                {"userId":"u-001","candidateId":"c-02","region":"norte",
                 "ip":"190.12.4.7","timestamp":"2026-11-14T15:30:00Z"}
                """);
        // Assert
        verify(service).onVote(vote.capture());
        assertThat(vote.getValue().userId().value()).isEqualTo("u-001");
        assertThat(vote.getValue().candidateId().value()).isEqualTo("c-02");
        assertThat(vote.getValue().timestamp()).isEqualTo(Instant.parse("2026-11-14T15:30:00Z"));
    }

    @Test
    void shouldIgnoreMalformedEvents_andContinueConsuming() {
        // Arrange
        var listener = new RawVotesKafkaListener(new VoteProcessorConfiguration().objectMapper(), service);
        // Act
        listener.onMessage("{broken");
        listener.onMessage("{}");
        listener.onMessage("null");
        listener.onMessage(null);
        listener.onMessage("""
                {"userId":"u-001","candidateId":"c-02","region":"norte",
                 "ip":"190.12.4.7","timestamp":"2026-11-14T15:30:00Z"}
                """);
        // Assert
        verify(service, times(1)).onVote(any(VoteEvent.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    void shouldPropagateServiceFailures_whenMessageIsValid() {
        // Arrange
        var listener = new RawVotesKafkaListener(new VoteProcessorConfiguration().objectMapper(), service);
        doThrow(new IllegalStateException("service failed")).when(service).onVote(any());
        // Act
        var failure = catchThrowable(() -> listener.onMessage("""
                {"userId":"u-001","candidateId":"c-02","region":"norte",
                 "ip":"190.12.4.7","timestamp":"2026-11-14T15:30:00Z"}
                """));
        // Assert
        assertThat(failure).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldPublishToGlobalFanout_whenSnapshotIsReady() {
        // Arrange
        var publisher = new RabbitResultsPublisher(rabbit);
        var snapshot = new TallySnapshot(Map.of("c-02", 1L), 1, Instant.EPOCH);
        // Act
        publisher.publishGlobal(snapshot);
        // Assert
        verify(rabbit).convertAndSend("live_results_global", "", snapshot);
        verifyNoMoreInteractions(rabbit);
    }

    @Test
    void shouldSerializeGlobalContract_withIsoTimeAndJsonContentType() throws Exception {
        // Arrange
        var config = new VoteProcessorConfiguration();
        var mapper = config.objectMapper();
        var converter = config.rabbitMessageConverter(mapper);
        var snapshot = new TallySnapshot(Map.of("c-02", 1L), 1, Instant.EPOCH);
        // Act
        var message = converter.toMessage(snapshot, new MessageProperties());
        var json = mapper.readTree(message.getBody());
        // Assert
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(json.size()).isEqualTo(3);
        assertThat(json.path("totals").path("c-02").asLong()).isEqualTo(1);
        assertThat(json.path("totalVotes").asLong()).isEqualTo(1);
        assertThat(json.path("at").asText()).isEqualTo("1970-01-01T00:00:00Z");
        assertThat(config.globalResultsExchange().isDurable()).isTrue();
        assertThat(config.globalResultsExchange().getType()).isEqualTo("fanout");
    }

    @Test
    void shouldDelegateSnapshotPublication_whenSchedulerRuns() {
        // Arrange
        var scheduler = new SnapshotScheduler(service);
        // Act
        scheduler.publishSnapshot();
        // Assert
        verify(service, times(1)).publishSnapshot();
        verifyNoMoreInteractions(service);
    }
}