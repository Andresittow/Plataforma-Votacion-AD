package com.votingplatform.common.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Objects;
import java.util.Locale;
import java.util.Set;

public record Region(String value) {
    private static final Set<String> VALID_REGIONS = Set.of("norte", "sur", "este", "oeste", "centro");

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public Region {
        value = Objects.requireNonNull(value, "region must not be null").trim().toLowerCase(Locale.ROOT);
        if (!VALID_REGIONS.contains(value)) {
            throw new IllegalArgumentException("Unknown region: " + value);
        }
    }

    @JsonValue
    public String value() {
        return value;
    }
}
