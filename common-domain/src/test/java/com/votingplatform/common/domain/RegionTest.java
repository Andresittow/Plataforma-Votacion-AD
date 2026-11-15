package com.votingplatform.common.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class RegionTest {
    static Stream<String> invalidValues() {
        return Stream.of("", "   ",  "nor te", "invalid");
    }

    @Test
    void shouldRejectNull() {
        // Arrange
        String value = null;
        // Act
        var action = catchThrowable(() -> new Region(value));
        // Assert
        assertThat(action).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    void shouldRejectInvalidValue(String value) {
        // Arrange
        // Act
        var action = catchThrowable(() -> new Region(value));
        // Assert
        assertThat(action).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCompareByValue() {
        // Arrange
        var first = new Region("norte");
        var second = new Region("norte");
        // Act
        boolean equal = first.equals(second);
        // Assert
        assertThat(equal).isTrue();
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first.value()).isEqualTo("norte");
    }

    @Test
    void shouldNormalizeValue() {
        // Arrange
        String input = "  NORTE  ";
        // Act
        var value = new Region(input);
        // Assert
        assertThat(value.value()).isEqualTo("norte");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"norte", "sur", "este", "oeste", "centro"})
    void shouldAcceptContractRegion(String input) {
        // Arrange
        // Act
        var region = new Region(input);
        // Assert
        assertThat(region.value()).isEqualTo(input);
    }

}
