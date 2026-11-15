package com.votingplatform.regionalrollupservice.domain;
import com.votingplatform.common.domain.Region;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PopulationWeighting implements WeightingStrategy {
    private static final Set<Region> REGIONS = Set.of(new Region("norte"), new Region("sur"),
            new Region("este"), new Region("oeste"), new Region("centro"));
    private final Map<Region, Double> weights;

    public PopulationWeighting(Map<Region, Double> weights) {
        this.weights = Map.copyOf(Objects.requireNonNull(weights, "weights"));
        if (!this.weights.keySet().equals(REGIONS)) {
            throw new IllegalArgumentException("Weights must cover every region");
        }
        this.weights.values().forEach(weight -> {
            if (!Double.isFinite(weight) || weight <= 0) {
                throw new IllegalArgumentException("Weights must be finite and positive");
            }
        });
    }

    public String name() { return "PopulationWeighting"; }
    public double weightFor(Region region) {
        return weights.get(Objects.requireNonNull(region, "region"));
    }
}