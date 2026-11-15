package com.votingplatform.voterregistrationservice.application;

import com.votingplatform.common.domain.Region;
import com.votingplatform.common.domain.VoterId;
import com.votingplatform.common.domain.VoterRecord;
import com.votingplatform.voterregistrationservice.application.port.EligibleVoterPublisher;
import com.votingplatform.voterregistrationservice.application.port.RawVoter;
import com.votingplatform.voterregistrationservice.application.port.VoterSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VoterRegistrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoterRegistrationService.class);

    private final VoterSource source;
    private final EligibleVoterPublisher publisher;

    public VoterRegistrationService(VoterSource source, EligibleVoterPublisher publisher) {
        this.source = source;
        this.publisher = publisher;
    }

    public RegistrationReport registerAll() {
        List<RawVoter> rawVoters = source.readAll();
        Map<VoterId, VoterRecord> validRecords = new LinkedHashMap<>();
        int rejected = 0;

        for (RawVoter rawVoter : rawVoters) {
            try {
                VoterRecord record = toRecord(rawVoter);
                if (validRecords.containsKey(record.userId())) {
                    rejected++;
                    LOGGER.warn("Duplicate voter {}, last record wins", record.userId().value());
                }
                validRecords.put(record.userId(), record);
            } catch (RuntimeException exception) {
                rejected++;
                LOGGER.warn("Rejected voter registration {}: {}", rawVoter, exception.getMessage());
            }
        }

        validRecords.values().forEach(publisher::publish);
        return new RegistrationReport(validRecords.size(), rejected);
    }

    private VoterRecord toRecord(RawVoter rawVoter) {
        if (rawVoter == null) {
            throw new IllegalArgumentException("Raw voter must not be null");
        }
        return new VoterRecord(new VoterId(rawVoter.userId()), new Region(rawVoter.region()), true);
    }
}
