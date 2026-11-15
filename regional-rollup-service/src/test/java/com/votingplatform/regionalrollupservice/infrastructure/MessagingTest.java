package com.votingplatform.regionalrollupservice.infrastructure;

import com.votingplatform.common.domain.VoteEvent;
import com.votingplatform.regionalrollupservice.application.RegionalRollupService;
import com.votingplatform.regionalrollupservice.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.properties.bind.*;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import java.time.Instant;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessagingTest {
    @Mock RegionalRollupService service;
    @Mock RabbitTemplate rabbit;
    @Captor ArgumentCaptor<VoteEvent> vote;

    private static final String VALID = """
            {"userId":"u-001","candidateId":"c-02","region":"norte",
             "ip":"190.12.4.7","timestamp":"2026-11-14T15:30:00Z"}
            """;

    @Test
    void shouldDeserializeFlatContract_andIgnoreMalformedMessages() {
        // Arrange
        var listener = new RawVotesKafkaListener(new RegionalRollupConfiguration().objectMapper(), service);
        // Act
        listener.onMessage("{broken");
        listener.onMessage("{}");
        listener.onMessage("null");
        listener.onMessage(null);
        listener.onMessage(VALID);
        // Assert
        verify(service, times(1)).onVote(vote.capture());
        verifyNoMoreInteractions(service);
        assertThat(vote.getValue().userId().value()).isEqualTo("u-001");
        assertThat(vote.getValue().region().value()).isEqualTo("norte");
        assertThat(vote.getValue().timestamp()).isEqualTo(Instant.parse("2026-11-14T15:30:00Z"));
    }

    @Test
    void shouldPropagateServiceFailure_whenVoteIsValid() {
        // Arrange
        var listener = new RawVotesKafkaListener(new RegionalRollupConfiguration().objectMapper(), service);
        doThrow(new IllegalStateException("failed")).when(service).onVote(any());
        // Act
        var failure = catchThrowable(() -> listener.onMessage(VALID));
        // Assert
        assertThat(failure).isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"norte", "sur", "este", "oeste", "centro"})
    void shouldRouteSnapshotToItsRegion(String region) {
        // Arrange
        var snapshot = new RegionalSnapshot(region, Map.of("c1", 2L), Map.of("c1", 2.4),
                "PopulationWeighting", Instant.EPOCH);
        var publisher = new RabbitRegionalResultsPublisher(rabbit);
        // Act
        publisher.publishRegional(snapshot);
        // Assert
        verify(rabbit).convertAndSend("live_results_regional", "results." + region, snapshot);
        verifyNoMoreInteractions(rabbit);
    }

    @Test
    void shouldSerializeRegionalContract_withIsoTimestamp() throws Exception {
        // Arrange
        var config = new RegionalRollupConfiguration();
        var mapper = config.objectMapper();
        var snapshot = new RegionalSnapshot("norte", Map.of("c1", 2L), Map.of("c1", 2.4),
                "PopulationWeighting", Instant.EPOCH);
        // Act
        var message = config.rabbitMessageConverter(mapper).toMessage(snapshot, new MessageProperties());
        var json = mapper.readTree(message.getBody());
        // Assert
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(json).isEqualTo(mapper.readTree("""
                {"region":"norte","totals":{"c1":2},"weightedTotals":{"c1":2.4},
                 "strategy":"PopulationWeighting","at":"1970-01-01T00:00:00Z"}
                """));
        assertThat(config.regionalResultsExchange().getName()).isEqualTo("live_results_regional");
        assertThat(config.regionalResultsExchange().getType()).isEqualTo("topic");
        assertThat(config.regionalResultsExchange().isDurable()).isTrue();
    }

    @Test
    void shouldDelegateToService_whenScheduled() {
        // Arrange
        var scheduler = new SnapshotScheduler(service);
        // Act
        scheduler.publishSnapshots();
        // Assert
        verify(service).publishSnapshots();
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"uniform", "population"})
    void shouldBindProperties_andSelectStrategy(String name) {
        // Arrange
        var source = new MapConfigurationPropertySource(Map.of(
                "rollup.strategy", name, "rollup.weights.norte", "1.2",
                "rollup.weights.sur", "0.9", "rollup.weights.este", "1.0",
                "rollup.weights.oeste", "1.1", "rollup.weights.centro", "1.0"));
        var properties = new Binder(source).bind("rollup", Bindable.of(RollupProperties.class)).get();
        // Act
        var strategy = new WeightingConfiguration().weightingStrategy(properties);
        // Assert
        assertThat(strategy.name()).isEqualTo(Map.of("uniform", "UniformWeighting",
                "population", "PopulationWeighting").get(name));
    }

    @Test
    void shouldRejectUnknownStrategy_andMissingPopulationWeights() {
        // Arrange
        var config = new WeightingConfiguration();
        // Act
        var unknown = catchThrowable(() -> config.weightingStrategy(new RollupProperties("other", Map.of())));
        var incomplete = catchThrowable(() -> config.weightingStrategy(new RollupProperties("population", Map.of())));
        // Assert
        assertThat(unknown).isInstanceOf(IllegalArgumentException.class);
        assertThat(incomplete).isInstanceOf(IllegalArgumentException.class);
    }
}