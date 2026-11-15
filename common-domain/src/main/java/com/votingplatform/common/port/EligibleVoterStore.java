package com.votingplatform.common.port;

import com.votingplatform.common.domain.VoterId;
import com.votingplatform.common.domain.VoterRecord;
import java.util.Optional;

public interface EligibleVoterStore {
    boolean isEligible(VoterId id);

    Optional<VoterRecord> find(VoterId id);

    void apply(VoterRecord record);

    void remove(VoterId id);
}
