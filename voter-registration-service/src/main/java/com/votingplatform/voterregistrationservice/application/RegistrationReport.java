package com.votingplatform.voterregistrationservice.application;

public record RegistrationReport(int publicados, int rechazados) {
    public RegistrationReport {
        if (publicados < 0 || rechazados < 0) {
            throw new IllegalArgumentException("Report counts must not be negative");
        }
    }
}
