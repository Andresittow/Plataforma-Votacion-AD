package com.votingplatform.voterregistrationservice.application.port;

import java.util.List;

public interface VoterSource {
    List<RawVoter> readAll();
}
