package com.votingplatform.regionalrollupservice.domain;
import com.votingplatform.common.domain.Region;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record RegionalSnapshot(String region, Map<String, Long> totals,
        Map<String, Double> weightedTotals, String strategy, Instant at) {
    public RegionalSnapshot {
        region = new Region(region).value();
        totals = Map.copyOf(Objects.requireNonNull(totals, "totals"));
        weightedTotals = Map.copyOf(Objects.requireNonNull(weightedTotals, "weightedTotals"));
        Objects.requireNonNull(strategy, "strategy");
        Objects.requireNonNull(at, "at");
        if (strategy.isBlank() || !totals.keySet().equals(weightedTotals.keySet())) {
            throw new IllegalArgumentException("Strategy and matching totals are required");
        }
        totals.forEach((candidate, count) -> {
            if (candidate.isBlank() || count <= 0) {
                throw new IllegalArgumentException("Raw totals must be positive");
            }
        });
        weightedTotals.values().forEach(count -> {
            if (!Double.isFinite(count) || count <= 0) {
                throw new IllegalArgumentException("Weighted totals must be finite and positive");
            }
        });
    }
}