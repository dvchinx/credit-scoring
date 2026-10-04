package com.florez.backend.scoring.domain.model;

import java.util.List;

public record PolicyResult(DecisionOutcome outcome, List<String> reasons) {

    public PolicyResult {
        reasons = List.copyOf(reasons);
    }
}
