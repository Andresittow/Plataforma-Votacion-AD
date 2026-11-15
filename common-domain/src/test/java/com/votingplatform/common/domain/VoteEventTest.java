package com.votingplatform.common.domain;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.*;

class VoteEventTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    void shouldRejectEachNullComponent(int missing) {
        // Arrange
        var id = new VoterId("u-001");
        var candidate = new CandidateId("c-02");
        var region = new Region("norte");
        var ip = new IpAddress("190.12.4.7");
        var timestamp = Instant.parse("2026-11-14T15:30:00Z");
        // Act
        var action = catchThrowable(() -> new VoteEvent(
            missing == 0 ? null : id, missing == 1 ? null : candidate,
            missing == 2 ? null : region, missing == 3 ? null : ip,
            missing == 4 ? null : timestamp));
        // Assert
        assertThat(action).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldCompareByValue() {
        // Arrange
        var first = new VoteEvent(new VoterId("u-001"), new CandidateId("c-02"),
            new Region("norte"), new IpAddress("190.12.4.7"), Instant.parse("2026-11-14T15:30:00Z"));
        var second = new VoteEvent(new VoterId("u-001"), new CandidateId("c-02"),
            new Region("norte"), new IpAddress("190.12.4.7"), Instant.parse("2026-11-14T15:30:00Z"));
        // Act
        boolean equal = first.equals(second);
        // Assert
        assertThat(equal).isTrue();
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }
}
