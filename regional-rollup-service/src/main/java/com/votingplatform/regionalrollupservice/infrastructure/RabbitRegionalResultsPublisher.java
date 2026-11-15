package com.votingplatform.regionalrollupservice.infrastructure;
import com.votingplatform.regionalrollupservice.application.port.RegionalResultsPublisher;
import com.votingplatform.regionalrollupservice.domain.RegionalSnapshot;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public final class RabbitRegionalResultsPublisher implements RegionalResultsPublisher {
    public static final String EXCHANGE = "live_results_regional";
    private final RabbitTemplate rabbit;
    public RabbitRegionalResultsPublisher(RabbitTemplate rabbit) { this.rabbit = rabbit; }

    public void publishRegional(RegionalSnapshot snapshot) {
        rabbit.convertAndSend(EXCHANGE, "results." + snapshot.region(), snapshot);
    }
}