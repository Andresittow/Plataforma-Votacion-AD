package com.votingplatform.regionalrollupservice.infrastructure;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rollup")
public record RollupProperties(String strategy, Map<String, Double> weights) {
    public RollupProperties {
        Objects.requireNonNull(strategy, "rollup.strategy");
        weights = weights == null ? Map.of() : Map.copyOf(weights);
    }
}