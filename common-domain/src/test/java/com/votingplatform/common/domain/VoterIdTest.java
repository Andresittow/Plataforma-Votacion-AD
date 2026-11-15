package com.votingplatform.common.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class VoterIdTest {
    static Stream<String> invalidValues() {
        return Stream.of("", "  ",  "u 001", "u\t001", "u\n001", "u\u2003001", "a".repeat(65));
    }

    @Test
    void shouldRejectNull() {
        // Arrange
        String value = null;
        // Act
        var action = catchThrowable(() -> new VoterId(value));
        // Assert
        assertThat(action).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    void shouldRejectInvalidValue(String value) {
        // Arrange
        // Act
        var action = catchThrowable(() -> new VoterId(value));
        // Assert
        assertThat(action).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCompareByValue() {
        // Arrange
        var first = new VoterId("u-001");
        var second = new VoterId("u-001");
        // Act
        boolean equal = first.equals(second);
        // Assert
        assertThat(equal).isTrue();
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first.value()).isEqualTo("u-001");
    }

    @Test
    void shouldNormalizeValue() {
        // Arrange
        String input = "  u-001  ";
        // Act
        var value = new VoterId(input);
        // Assert
        assertThat(value.value()).isEqualTo("u-001");
    }

    @Test
    void shouldAcceptMaximumLength() {
        // Arrange
        String input = "a".repeat(64);
        // Act
        var value = new VoterId(input);
        // Assert
        assertThat(value.value()).isEqualTo(input);
    }

}
