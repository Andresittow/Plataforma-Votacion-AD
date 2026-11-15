package com.votingplatform.common.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

class IpAddressTest {
    static Stream<String> invalidValues() {
        return Stream.of("", " ",  "localhost", "256.1.1.1", "1.2.3", "1.2.3.4.5", "01.2.3.4", "2001:::1", "gggg::1", "[::1]", "fe80::1%eth0", " ::1");
    }

    @Test
    void shouldRejectNull() {
        // Arrange
        String value = null;
        // Act
        var action = catchThrowable(() -> new IpAddress(value));
        // Assert
        assertThat(action).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    void shouldRejectInvalidValue(String value) {
        // Arrange
        // Act
        var action = catchThrowable(() -> new IpAddress(value));
        // Assert
        assertThat(action).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldCompareByValue() {
        // Arrange
        var first = new IpAddress("190.12.4.7");
        var second = new IpAddress("190.12.4.7");
        // Act
        boolean equal = first.equals(second);
        // Assert
        assertThat(equal).isTrue();
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
        assertThat(first.value()).isEqualTo("190.12.4.7");
    }

    @ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {
        "0.0.0.0", "255.255.255.255", "::", "::1", "2001:db8::1",
        "2001:db8:0:0:0:0:2:1", "::ffff:192.0.2.128"
    })
    void shouldAcceptIpLiteral(String value) {
        // Arrange
        // Act
        var address = new IpAddress(value);
        // Assert
        assertThat(address.value()).isEqualTo(value);
    }

}
