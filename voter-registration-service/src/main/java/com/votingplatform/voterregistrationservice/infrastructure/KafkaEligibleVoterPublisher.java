package com.votingplatform.voterregistrationservice.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.votingplatform.common.domain.VoterRecord;
import com.votingplatform.voterregistrationservice.application.port.EligibleVoterPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public final class KafkaEligibleVoterPublisher implements EligibleVoterPublisher {
    static final String TOPIC = "eligible_voters";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEligibleVoterPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(VoterRecord record) {
        try {
            kafkaTemplate.send(TOPIC, record.userId().value(), objectMapper.writeValueAsString(record));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize voter record", exception);
        }
    }
}
