package com.votingplatform.common.port;

import com.votingplatform.common.domain.VoteEvent;

public interface VotePublisher {
    void publish(VoteEvent vote);
}
