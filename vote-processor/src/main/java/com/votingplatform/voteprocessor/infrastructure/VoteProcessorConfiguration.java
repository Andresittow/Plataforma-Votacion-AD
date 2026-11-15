package com.votingplatform.voteprocessor.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.votingplatform.voteprocessor.application.VoteProcessingService;
import com.votingplatform.voteprocessor.application.port.ResultsPublisher;
import com.votingplatform.voteprocessor.domain.GlobalTally;
import java.time.Clock;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableKafka
@EnableScheduling
public class VoteProcessorConfiguration {
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
    public FanoutExchange globalResultsExchange() {
        return new FanoutExchange(RabbitResultsPublisher.EXCHANGE, true, false);
    }

    @Bean
    public GlobalTally globalTally() {
        return new GlobalTally();
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public VoteProcessingService voteProcessingService(GlobalTally tally, ResultsPublisher publisher, Clock clock) {
        return new VoteProcessingService(tally, publisher, clock);
    }
}