package com.votingplatform.voterregistrationservice.infrastructure;

import com.votingplatform.voterregistrationservice.application.RegistrationReport;
import com.votingplatform.voterregistrationservice.application.VoterRegistrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public final class VoterRegistrationRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(VoterRegistrationRunner.class);

    private final VoterRegistrationService registrationService;

    public VoterRegistrationRunner(VoterRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        RegistrationReport report = registrationService.registerAll();
        LOGGER.info("Voter registration completed: published={}, rejected={}",
                report.publicados(), report.rechazados());
    }
}
