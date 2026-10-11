package com.florez.backend.modelregistry.domain.model;

import java.util.Objects;

/**
 * Datos con los que se entrenó una versión de modelo.
 *
 * @param sha256      huella del contenido del dataset: permite saber si dos versiones se
 *                    entrenaron exactamente con los mismos datos
 * @param defaultRate tasa de default observada en el set de entrenamiento
 */
public record TrainingDataset(String source, String sha256, int trainingRows, int testRows, double defaultRate) {

    public TrainingDataset {
        Objects.requireNonNull(source, "source");
        ModelArtifact.requireSha256("dataset.sha256", sha256);
        if (trainingRows <= 0 || testRows <= 0) {
            throw new IllegalArgumentException("El dataset debe tener filas de entrenamiento y de test");
        }
    }
}
