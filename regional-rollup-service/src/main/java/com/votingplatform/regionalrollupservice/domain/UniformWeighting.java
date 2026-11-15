package com.votingplatform.regionalrollupservice.domain;
import com.votingplatform.common.domain.Region;
import java.util.Objects;

public final class UniformWeighting implements WeightingStrategy {
    public String name() { return "UniformWeighting"; }
    public double weightFor(Region region) {
        Objects.requireNonNull(region, "region");
        return 1.0;
    }
}