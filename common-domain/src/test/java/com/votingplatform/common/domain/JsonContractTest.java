package com.votingplatform.common.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JsonContractTest {
    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void shouldRoundTripVoteWithExactFlatContractJson() throws Exception {
        // Arrange
        var vote = new VoteEvent(new VoterId("u-001"), new CandidateId("c-02"),
            new Region("norte"), new IpAddress("190.12.4.7"), Instant.parse("2026-11-14T15:30:00Z"));
        String expected = "{\"userId\":\"u-001\",\"candidateId\":\"c-02\",\"region\":\"norte\",\"ip\":\"190.12.4.7\",\"timestamp\":\"2026-11-14T15:30:00Z\"}";
        // Act
        String json = mapper.writeValueAsString(vote);
        var restored = mapper.readValue(json, VoteEvent.class);
        // Assert
        assertThat(json).isEqualTo(expected);
        assertThat(restored).isEqualTo(vote);
    }

    @Test
    void shouldRoundTripVoterWithExactFlatContractJson() throws Exception {
        // Arrange
        var voter = new VoterRecord(new VoterId("u-001"), new Region("norte"), true);
        String expected = "{\"userId\":\"u-001\",\"region\":\"norte\",\"active\":true}";
        // Act
        String json = mapper.writeValueAsString(voter);
        var restored = mapper.readValue(json, VoterRecord.class);
        // Assert
        assertThat(json).isEqualTo(expected);
        assertThat(restored).isEqualTo(voter);
    }
}
