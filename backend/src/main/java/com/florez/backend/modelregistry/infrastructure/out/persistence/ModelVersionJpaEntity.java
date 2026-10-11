package com.florez.backend.modelregistry.infrastructure.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * Fila append-only de {@code model_versions}. El id es la versión (asignada, no generada);
 * {@link Persistable#isNew()} siempre es true para que Spring Data haga INSERT y nunca merge.
 */
@Entity
@Immutable
@Table(name = "model_versions")
public class ModelVersionJpaEntity implements Persistable<String> {

    @Id
    private String version;

    @Column(nullable = false)
    private String algorithm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String hyperparameters;

    @Column(name = "trained_at", nullable = false)
    private Instant trainedAt;

    @Column(name = "dataset_source", nullable = false)
    private String datasetSource;

    @Column(name = "dataset_sha256", nullable = false)
    private String datasetSha256;

    @Column(name = "training_rows", nullable = false)
    private Integer trainingRows;

    @Column(name = "test_rows", nullable = false)
    private Integer testRows;

    @Column(name = "default_rate", nullable = false)
    private Double defaultRate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String metrics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feature_names", nullable = false)
    private String featureNames;

    @Column(name = "artifact_sha256", nullable = false)
    private String artifactSha256;

    @Column(name = "registered_by", nullable = false)
    private String registeredBy;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Transient
    private boolean isNew;

    protected ModelVersionJpaEntity() {
        // requerido por JPA
    }

    public ModelVersionJpaEntity(
            String version,
            String algorithm,
            String hyperparameters,
            Instant trainedAt,
            String datasetSource,
            String datasetSha256,
            Integer trainingRows,
            Integer testRows,
            Double defaultRate,
            String metrics,
            String featureNames,
            String artifactSha256,
            String registeredBy,
            Instant registeredAt) {
        this.version = version;
        this.algorithm = algorithm;
        this.hyperparameters = hyperparameters;
        this.trainedAt = trainedAt;
        this.datasetSource = datasetSource;
        this.datasetSha256 = datasetSha256;
        this.trainingRows = trainingRows;
        this.testRows = testRows;
        this.defaultRate = defaultRate;
        this.metrics = metrics;
        this.featureNames = featureNames;
        this.artifactSha256 = artifactSha256;
        this.registeredBy = registeredBy;
        this.registeredAt = registeredAt;
        this.isNew = true;
    }

    @Override
    public String getId() {
        return version;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getHyperparameters() {
        return hyperparameters;
    }

    public Instant getTrainedAt() {
        return trainedAt;
    }

    public String getDatasetSource() {
        return datasetSource;
    }

    public String getDatasetSha256() {
        return datasetSha256;
    }

    public Integer getTrainingRows() {
        return trainingRows;
    }

    public Integer getTestRows() {
        return testRows;
    }

    public Double getDefaultRate() {
        return defaultRate;
    }

    public String getMetrics() {
        return metrics;
    }

    public String getFeatureNames() {
        return featureNames;
    }

    public String getArtifactSha256() {
        return artifactSha256;
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }
}
