package com.votingplatform.voterregistrationservice.infrastructure;

import com.votingplatform.voterregistrationservice.application.VoterRegistrationService;
import com.votingplatform.voterregistrationservice.application.port.EligibleVoterPublisher;
import com.votingplatform.voterregistrationservice.application.port.VoterSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VoterRegistrationConfiguration {
    @Bean
    public VoterRegistrationService voterRegistrationService(VoterSource source,
                                                             EligibleVoterPublisher publisher) {
        return new VoterRegistrationService(source, publisher);
    }
}
