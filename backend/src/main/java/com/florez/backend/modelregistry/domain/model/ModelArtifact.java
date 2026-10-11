package com.florez.backend.modelregistry.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Metadata de un modelo entrenado tal como la reporta el servicio de ML. Una versión solo puede
 * registrarse si su metadata está completa: sin dataset, métricas o hash del artefacto no hay
 * trazabilidad.
 */
public record ModelArtifact(
        String version,
        String algorithm,
        Map<String, Object> hyperparameters,
        Instant trainedAt,
        TrainingDataset dataset,
        Map<String, Double> metrics,
        List<String> featureNames,
        String artifactSha256) {

    public static final Pattern VERSION_FORMAT = Pattern.compile("^[0-9A-Za-z_-]{1,100}$");
    private static final Pattern SHA256_FORMAT = Pattern.compile("^[0-9a-f]{64}$");

    public ModelArtifact {
        if (version == null || !VERSION_FORMAT.matcher(version).matches()) {
            throw new IllegalArgumentException("Versión de modelo inválida: " + version);
        }
        Objects.requireNonNull(algorithm, "algorithm");
        Objects.requireNonNull(trainedAt, "trainedAt");
        Objects.requireNonNull(dataset, "dataset");
        if (metrics == null || metrics.isEmpty()) {
            throw new IllegalArgumentException("La versión " + version + " no reporta métricas");
        }
        if (featureNames == null || featureNames.isEmpty()) {
            throw new IllegalArgumentException("La versión " + version + " no reporta sus variables de entrada");
        }
        requireSha256("artifactSha256", artifactSha256);
        // Copia que admite valores null: un hiperparámetro en None (p. ej. random_state) es parte
        // de la configuración real del modelo y debe conservarse.
        hyperparameters = hyperparameters == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(hyperparameters));
        metrics = Map.copyOf(metrics);
        featureNames = List.copyOf(featureNames);
    }

    static void requireSha256(String name, String value) {
        if (value == null || !SHA256_FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " debe ser un SHA-256 en hexadecimal: " + value);
        }
    }
}
