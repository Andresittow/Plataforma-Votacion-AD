package com.votingplatform.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;

public record VoterId(String value) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public VoterId {
        value = Objects.requireNonNull(value, "VoterId must not be null").trim();
        if (value.isEmpty() || value.length() > 64
                || value.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) {
            throw new IllegalArgumentException("VoterId must contain 1 to 64 characters without whitespace");
        }
    }

    @JsonValue
    public String value() {
        return value;
    }
}
