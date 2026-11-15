package com.votingplatform.regionalrollupservice.domain;
import com.votingplatform.common.domain.Region;

public interface WeightingStrategy {
    String name();
    double weightFor(Region region);
}