package com.votingplatform.regionalrollupservice.infrastructure;
import com.votingplatform.common.domain.Region;
import com.votingplatform.regionalrollupservice.domain.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RollupProperties.class)
public class WeightingConfiguration {
    @Bean
    public WeightingStrategy weightingStrategy(RollupProperties properties) {
        Map<String, Supplier<WeightingStrategy>> strategies = Map.of(
                "uniform", UniformWeighting::new,
                "population", () -> {
                    Map<Region, Double> weights = new HashMap<>();
                    properties.weights().forEach((region, weight) -> weights.put(new Region(region), weight));
                    return new PopulationWeighting(weights);
                });
        var factory = strategies.get(properties.strategy());
        if (factory == null) throw new IllegalArgumentException("rollup.strategy must be uniform or population");
        return factory.get();
    }
}