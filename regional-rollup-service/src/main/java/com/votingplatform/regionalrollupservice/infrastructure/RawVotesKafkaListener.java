package com.votingplatform.regionalrollupservice.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.votingplatform.common.domain.VoteEvent;
import com.votingplatform.regionalrollupservice.application.RegionalRollupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public final class RawVotesKafkaListener {
    private static final Logger LOG = LoggerFactory.getLogger(RawVotesKafkaListener.class);
    private final ObjectMapper mapper;
    private final RegionalRollupService service;

    public RawVotesKafkaListener(ObjectMapper mapper, RegionalRollupService service) {
        this.mapper = mapper;
        this.service = service;
    }

    @KafkaListener(topics = "raw_votes", groupId = "regional-rollup-group",
            properties = "auto.offset.reset=earliest")
    public void onMessage(String json) {
        final VoteEvent vote;
        try {
            vote = mapper.readValue(json, VoteEvent.class);
            if (vote == null) {
                LOG.warn("Ignoring null raw_votes event");
                return;
            }
        } catch (JsonProcessingException | IllegalArgumentException ex) {
            LOG.warn("Ignoring malformed raw_votes event ({})", ex.getClass().getSimpleName());
            return;
        }
        service.onVote(vote);
    }
}