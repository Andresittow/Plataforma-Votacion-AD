package com.votingplatform.regionalrollupservice.application.port;
import com.votingplatform.regionalrollupservice.domain.RegionalSnapshot;

public interface RegionalResultsPublisher {
    void publishRegional(RegionalSnapshot snapshot);
}