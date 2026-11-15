package com.votingplatform.regionalrollupservice.domain;

import com.votingplatform.common.domain.Region;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class WeightingStrategyTest {
    static Map<Region, Double> weights() {
        return new HashMap<>(Map.of(new Region("norte"), 1.2, new Region("sur"), .9,
                new Region("este"), 1.0, new Region("oeste"), 1.1, new Region("centro"), 1.0));
    }

    @Test
    void shouldUseUniformWeights_forEveryRegion() {
        // Arrange
        var strategy = new UniformWeighting();
        // Act
        var actual = weights().keySet().stream().map(strategy::weightFor).toList();
        // Assert
        assertThat(actual).containsOnly(1.0);
        assertThat(strategy.name()).isEqualTo("UniformWeighting");
        assertThatNullPointerException().isThrownBy(() -> strategy.weightFor(null));
    }

    @Test
    void shouldUseConfiguredWeights_andCopyConfiguration() {
        // Arrange
        var source = weights();
        var expected = Map.copyOf(source);
        var strategy = new PopulationWeighting(source);
        // Act
        source.clear();
        // Assert
        expected.forEach((region, weight) -> assertThat(strategy.weightFor(region)).isEqualTo(weight));
        assertThat(strategy.name()).isEqualTo("PopulationWeighting");
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void shouldRejectInvalidWeights_whenConfigured(double invalid) {
        // Arrange
        var weights = weights();
        weights.put(new Region("norte"), invalid);
        // Act
        var failure = catchThrowable(() -> new PopulationWeighting(weights));
        // Assert
        assertThat(failure).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectIncompleteOrNullConfiguration() {
        // Arrange
        var weights = weights();
        weights.remove(new Region("sur"));
        // Act
        var failure = catchThrowable(() -> new PopulationWeighting(weights));
        // Assert
        assertThat(failure).isInstanceOf(IllegalArgumentException.class);
        assertThatNullPointerException().isThrownBy(() -> new PopulationWeighting(null));
        weights.put(new Region("sur"), null);
        assertThatNullPointerException().isThrownBy(() -> new PopulationWeighting(weights));
    }
}