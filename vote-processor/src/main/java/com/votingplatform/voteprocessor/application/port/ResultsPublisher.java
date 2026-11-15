package com.votingplatform.voteprocessor.application.port;

import com.votingplatform.voteprocessor.domain.TallySnapshot;

public interface ResultsPublisher {
    void publishGlobal(TallySnapshot snapshot);
}