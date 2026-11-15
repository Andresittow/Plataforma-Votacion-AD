package com.votingplatform.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record CandidateId(String value) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public CandidateId {
        value = Objects.requireNonNull(value, "CandidateId must not be null").trim();
        if (value.isEmpty() || value.length() > 64
                || value.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new IllegalArgumentException("CandidateId must contain 1 to 64 characters without whitespace");
        }
    }

    @JsonValue
    public String value() {
        return value;
    }
}
