package com.votingplatform.common.domain;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class VoterRecordTest {
    @Test
    void shouldRejectNullComponents() {
        // Arrange
        var id = new VoterId("u-001");
        var region = new Region("norte");
        // Act
        var missingId = catchThrowable(() -> new VoterRecord(null, region, true));
        var missingRegion = catchThrowable(() -> new VoterRecord(id, null, true));
        // Assert
        assertThat(missingId).isInstanceOf(NullPointerException.class);
        assertThat(missingRegion).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldCompareByValueAndPreserveActiveFlag() {
        // Arrange
        var first = new VoterRecord(new VoterId("u-001"), new Region("norte"), false);
        var second = new VoterRecord(new VoterId("u-001"), new Region("norte"), false);
        // Act
        boolean equal = first.equals(second);
        // Assert
        assertThat(equal).isTrue();
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first.active()).isFalse();
        assertThat(first).isNotEqualTo(new VoterRecord(first.userId(), first.region(), true));
    }
}
