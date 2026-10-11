package com.florez.backend.modelregistry.domain.port.out;

import com.florez.backend.common.domain.ServiceUnavailableException;
import com.florez.backend.modelregistry.domain.exception.InvalidModelArtifactException;
import com.florez.backend.modelregistry.domain.model.CatalogEntry;
import java.util.Optional;

/** Versiones de modelo que el servicio de ML tiene disponibles en disco. */
public interface ModelArtifactCatalogPort {

    /**
     * @return vacío si el servicio de ML no tiene esa versión
     * @throws InvalidModelArtifactException si la metadata reportada está incompleta
     * @throws ServiceUnavailableException   si el servicio de ML no responde
     */
    Optional<CatalogEntry> find(String version);
}
