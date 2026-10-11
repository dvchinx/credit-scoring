package com.florez.backend.whatif.infrastructure.out.ml;

import static com.florez.backend.scoring.infrastructure.out.ml.MlServiceScoringAdapter.anyNull;

import com.florez.backend.scoring.domain.model.Explanation;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import com.florez.backend.scoring.domain.model.RiskAssessment;
import com.florez.backend.scoring.infrastructure.out.ml.MlContributionMapper;
import com.florez.backend.scoring.infrastructure.out.ml.MlServiceScoringAdapter;
import com.florez.backend.whatif.domain.model.ModelEvaluation;
import com.florez.backend.whatif.domain.port.out.WhatIfModelPort;
import com.florez.backend.whatif.infrastructure.out.ml.dto.MlEvaluation;
import com.florez.backend.whatif.infrastructure.out.ml.dto.MlWhatIfResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Cliente de {@code POST /what-if}: evalúa ambos escenarios con el mismo modelo cargado. */
@Component
public class MlServiceWhatIfAdapter implements WhatIfModelPort {

    private static final String PATH = "/what-if";

    private final RestClient restClient;

    public MlServiceWhatIfAdapter(RestClient mlServiceRestClient) {
        this.restClient = mlServiceRestClient;
    }

    @Override
    public Comparison compare(String modelVersion, ModelFeatures baseline, ModelFeatures scenario) {
        Map<String, Object> body = Map.of("baseline", baseline.asFeatureMap(), "scenario", scenario.asFeatureMap());
        MlWhatIfResponse response = MlServiceScoringAdapter.post(restClient, PATH, modelVersion, body, MlWhatIfResponse.class);
        if (anyNull(response.modelVersion(), response.baseline(), response.scenario())) {
            throw MlContributionMapper.invalid(PATH);
        }
        return new Comparison(
                toDomain(response.modelVersion(), response.baseline()),
                toDomain(response.modelVersion(), response.scenario()));
    }

    private static ModelEvaluation toDomain(String modelVersion, MlEvaluation evaluation) {
        if (anyNull(evaluation.probabilityOfDefault(), evaluation.riskScore(), evaluation.baseValue(),
                evaluation.outputValue())) {
            throw MlContributionMapper.invalid(PATH);
        }
        return new ModelEvaluation(
                new RiskAssessment(evaluation.probabilityOfDefault(), evaluation.riskScore(), modelVersion),
                new Explanation(modelVersion, evaluation.baseValue(), evaluation.outputValue(),
                        MlContributionMapper.toDomain(evaluation.contributions(), PATH)));
    }
}
