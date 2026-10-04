package com.florez.backend.scoring.domain.model;

import com.florez.backend.creditapplication.domain.model.ApplicationStatus;

public enum DecisionOutcome {
    APPROVED(ApplicationStatus.APPROVED),
    REJECTED(ApplicationStatus.REJECTED),
    MANUAL_REVIEW(ApplicationStatus.MANUAL_REVIEW);

    private final ApplicationStatus applicationStatus;

    DecisionOutcome(ApplicationStatus applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public ApplicationStatus toApplicationStatus() {
        return applicationStatus;
    }
}
