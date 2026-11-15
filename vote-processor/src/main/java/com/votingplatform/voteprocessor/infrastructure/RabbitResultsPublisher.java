package com.votingplatform.voteprocessor.infrastructure;

import com.votingplatform.voteprocessor.application.port.ResultsPublisher;
import com.votingplatform.voteprocessor.domain.TallySnapshot;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public final class RabbitResultsPublisher implements ResultsPublisher {
    public static final String EXCHANGE = "live_results_global";
    private final RabbitTemplate rabbitTemplate;

    public RabbitResultsPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void publishGlobal(TallySnapshot snapshot) {
        rabbitTemplate.convertAndSend(EXCHANGE, "", snapshot);
    }
}