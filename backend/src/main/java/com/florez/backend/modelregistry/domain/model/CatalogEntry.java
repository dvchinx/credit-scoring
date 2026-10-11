package com.florez.backend.modelregistry.domain.model;

/**
 * Una versión tal como la sirve hoy el servicio de ML.
 *
 * @param artifactVerified el servicio de ML comprobó que el archivo en disco coincide con el hash
 *                         calculado al entrenar
 */
public record CatalogEntry(ModelArtifact artifact, boolean artifactVerified) {
}
