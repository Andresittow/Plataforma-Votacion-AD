package com.votingplatform.regionalrollupservice.infrastructure;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.votingplatform.regionalrollupservice.application.RegionalRollupService;
import com.votingplatform.regionalrollupservice.application.port.RegionalResultsPublisher;
import com.votingplatform.regionalrollupservice.domain.*;
import java.time.Clock;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableKafka
@EnableScheduling
public class RegionalRollupConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build();
    }

    @Bean
    public Jackson2JsonMessageConverter rabbitMessageConverter(ObjectMapper mapper) {
        return new Jackson2JsonMessageConverter(mapper);
    }

    @Bean
    public TopicExchange regionalResultsExchange() {
        return new TopicExchange(RabbitRegionalResultsPublisher.EXCHANGE, true, false);
    }

    @Bean
    public RegionalTally regionalTally() { return new RegionalTally(); }

    @Bean
    public Clock clock() { return Clock.systemUTC(); }

    @Bean
    public RegionalRollupService regionalRollupService(RegionalTally tally, WeightingStrategy strategy,
            RegionalResultsPublisher publisher, Clock clock) {
        return new RegionalRollupService(tally, strategy, publisher, clock);
    }
}