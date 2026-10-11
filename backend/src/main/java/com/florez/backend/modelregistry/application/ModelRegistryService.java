package com.florez.backend.modelregistry.application;

import com.florez.backend.common.domain.BusinessRuleException;
import com.florez.backend.common.domain.DuplicateResourceException;
import com.florez.backend.common.domain.ResourceNotFoundException;
import com.florez.backend.modelregistry.domain.model.CatalogEntry;
import com.florez.backend.modelregistry.domain.model.ModelActivation;
import com.florez.backend.modelregistry.domain.model.ModelVersion;
import com.florez.backend.modelregistry.domain.port.in.ActivateModelVersionUseCase;
import com.florez.backend.modelregistry.domain.port.in.GetModelActivationsUseCase;
import com.florez.backend.modelregistry.domain.port.in.GetModelVersionsUseCase;
import com.florez.backend.modelregistry.domain.port.in.RegisterModelVersionUseCase;
import com.florez.backend.modelregistry.domain.port.out.ModelActivationRepositoryPort;
import com.florez.backend.modelregistry.domain.port.out.ModelArtifactCatalogPort;
import com.florez.backend.modelregistry.domain.port.out.ModelVersionRepositoryPort;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public final class ModelRegistryService implements
        RegisterModelVersionUseCase,
        ActivateModelVersionUseCase,
        GetModelVersionsUseCase,
        GetModelActivationsUseCase {

    private final ModelVersionRepositoryPort versionRepository;
    private final ModelActivationRepositoryPort activationRepository;
    private final ModelArtifactCatalogPort catalog;
    private final Set<String> supportedFeatureNames;
    private final Clock clock;

    /**
     * @param supportedFeatureNames variables que el backend sabe calcular para una solicitud; una
     *                              versión que espere otras no puede registrarse
     */
    public ModelRegistryService(
            ModelVersionRepositoryPort versionRepository,
            ModelActivationRepositoryPort activationRepository,
            ModelArtifactCatalogPort catalog,
            Set<String> supportedFeatureNames,
            Clock clock) {
        this.versionRepository = versionRepository;
        this.activationRepository = activationRepository;
        this.catalog = catalog;
        this.supportedFeatureNames = Set.copyOf(supportedFeatureNames);
        this.clock = clock;
    }

    @Override
    public ModelVersion register(String version, String registeredBy) {
        if (versionRepository.exists(version)) {
            throw new DuplicateResourceException("La versión de modelo '" + version + "' ya está registrada");
        }
        CatalogEntry entry = catalog.find(version)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El servicio de ML no tiene la versión de modelo '" + version + "'"));
        requireVerified(entry);

        Set<String> expectedFeatures = Set.copyOf(entry.artifact().featureNames());
        if (!expectedFeatures.equals(supportedFeatureNames)) {
            throw new BusinessRuleException("La versión '" + version + "' espera las variables "
                    + new TreeSet<>(expectedFeatures) + ", pero el backend calcula " + new TreeSet<>(supportedFeatureNames));
        }

        return versionRepository.save(new ModelVersion(entry.artifact(), registeredBy, clock.instant()));
    }

    @Override
    public ModelActivation activate(String version, String reason, String activatedBy) {
        ModelVersion registered = get(version);
        if (activationRepository.findCurrent().map(ModelActivation::modelVersion).filter(version::equals).isPresent()) {
            throw new BusinessRuleException("La versión '" + version + "' ya es la versión activa");
        }

        // El artefacto que va a servir el ML debe ser exactamente el que se registró.
        CatalogEntry served = catalog.find(version)
                .orElseThrow(() -> new BusinessRuleException(
                        "El servicio de ML ya no tiene el artefacto de la versión '" + version + "'"));
        requireVerified(served);
        String registeredHash = registered.artifact().artifactSha256();
        String servedHash = served.artifact().artifactSha256();
        if (!registeredHash.equals(servedHash)) {
            throw new BusinessRuleException("El artefacto que sirve el servicio de ML para '" + version
                    + "' (sha256 " + servedHash + ") no coincide con el registrado (sha256 " + registeredHash + ")");
        }

        return activationRepository.save(ModelActivation.create(version, reason, activatedBy, clock.instant()));
    }

    @Override
    public List<ModelVersion> list() {
        return versionRepository.findAllNewestFirst();
    }

    @Override
    public ModelVersion get(String version) {
        return versionRepository.findByVersion(version)
                .orElseThrow(() -> new ResourceNotFoundException("La versión de modelo '" + version + "' no está registrada"));
    }

    @Override
    public Optional<ModelActivation> current() {
        return activationRepository.findCurrent();
    }

    @Override
    public List<ModelActivation> history() {
        return activationRepository.findAllNewestFirst();
    }

    private static void requireVerified(CatalogEntry entry) {
        if (!entry.artifactVerified()) {
            throw new BusinessRuleException("El artefacto de la versión '" + entry.artifact().version()
                    + "' no coincide con el hash calculado al entrenarla: el archivo fue modificado");
        }
    }
}
