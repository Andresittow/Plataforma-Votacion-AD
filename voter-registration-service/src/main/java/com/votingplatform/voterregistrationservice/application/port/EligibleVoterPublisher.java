package com.votingplatform.voterregistrationservice.application.port;

import com.votingplatform.common.domain.VoterRecord;

public interface EligibleVoterPublisher {
    void publish(VoterRecord record);
}
