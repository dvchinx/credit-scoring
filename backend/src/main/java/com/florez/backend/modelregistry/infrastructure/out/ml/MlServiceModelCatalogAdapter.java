package com.florez.backend.modelregistry.infrastructure.out.ml;

import com.florez.backend.common.domain.ServiceUnavailableException;
import com.florez.backend.modelregistry.domain.exception.InvalidModelArtifactException;
import com.florez.backend.modelregistry.domain.model.CatalogEntry;
import com.florez.backend.modelregistry.domain.model.ModelArtifact;
import com.florez.backend.modelregistry.domain.model.TrainingDataset;
import com.florez.backend.modelregistry.domain.port.out.ModelArtifactCatalogPort;
import com.florez.backend.modelregistry.infrastructure.out.ml.dto.MlDatasetInfo;
import com.florez.backend.modelregistry.infrastructure.out.ml.dto.MlModelVersionInfo;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Consulta {@code GET /models/{version}} del servicio de ML. */
@Component
public class MlServiceModelCatalogAdapter implements ModelArtifactCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(MlServiceModelCatalogAdapter.class);

    private final RestClient restClient;

    public MlServiceModelCatalogAdapter(RestClient mlServiceRestClient) {
        this.restClient = mlServiceRestClient;
    }

    @Override
    public Optional<CatalogEntry> find(String version) {
        if (!ModelArtifact.VERSION_FORMAT.matcher(version).matches()) {
            return Optional.empty();
        }
        MlModelVersionInfo info;
        try {
            info = restClient.get().uri("/models/{version}", version).retrieve().body(MlModelVersionInfo.class);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            log.warn("Fallo consultando la versión {} en el servicio de ML: {}", version, e.getMessage());
            throw new ServiceUnavailableException("El servicio de ML no respondió al consultar la versión '" + version + "'", e);
        }
        if (info == null) {
            throw new ServiceUnavailableException("El servicio de ML devolvió una respuesta vacía para la versión '" + version + "'");
        }
        return Optional.of(new CatalogEntry(toArtifact(version, info), Boolean.TRUE.equals(info.artifactVerified())));
    }

    private static ModelArtifact toArtifact(String version, MlModelVersionInfo info) {
        List<String> missing = missingFields(info);
        if (!missing.isEmpty()) {
            throw new InvalidModelArtifactException("La versión '" + version + "' tiene metadata incompleta (falta: "
                    + String.join(", ", missing) + "). Reentrena el modelo para generar una versión registrable.");
        }
        MlDatasetInfo dataset = info.dataset();
        try {
            return new ModelArtifact(
                    info.version(),
                    info.algorithm(),
                    info.hyperparameters(),
                    parseInstant(info.trainedAt()),
                    new TrainingDataset(dataset.source(), dataset.sha256(), dataset.trainingRows(), dataset.testRows(),
                            dataset.defaultRate()),
                    info.metrics(),
                    info.featureNames(),
                    info.artifactSha256());
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new InvalidModelArtifactException("La versión '" + version + "' tiene metadata inválida: " + e.getMessage());
        }
    }

    private static List<String> missingFields(MlModelVersionInfo info) {
        List<String> missing = new ArrayList<>();
        addIfMissing(missing, "version", info.version());
        addIfMissing(missing, "trained_at", info.trainedAt());
        addIfMissing(missing, "algorithm", info.algorithm());
        addIfMissing(missing, "metrics", info.metrics() == null || info.metrics().isEmpty() ? null : info.metrics());
        addIfMissing(missing, "feature_names",
                info.featureNames() == null || info.featureNames().isEmpty() ? null : info.featureNames());
        addIfMissing(missing, "artifact_sha256", info.artifactSha256());
        MlDatasetInfo dataset = info.dataset();
        if (dataset == null) {
            missing.add("dataset");
        } else {
            addIfMissing(missing, "dataset.source", dataset.source());
            addIfMissing(missing, "dataset.sha256", dataset.sha256());
            addIfMissing(missing, "dataset.training_rows", dataset.trainingRows());
            addIfMissing(missing, "dataset.test_rows", dataset.testRows());
            addIfMissing(missing, "dataset.default_rate", dataset.defaultRate());
        }
        return missing;
    }

    private static void addIfMissing(List<String> missing, String name, Object value) {
        if (value == null) {
            missing.add(name);
        }
    }

    private static Instant parseInstant(String isoDateTime) {
        return OffsetDateTime.parse(isoDateTime).toInstant();
    }
}
