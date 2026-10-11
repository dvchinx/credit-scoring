package com.florez.backend.modelregistry.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.florez.backend.common.domain.BusinessRuleException;
import com.florez.backend.common.domain.DuplicateResourceException;
import com.florez.backend.common.domain.ResourceNotFoundException;
import com.florez.backend.modelregistry.ModelRegistryTestData;
import com.florez.backend.modelregistry.domain.model.CatalogEntry;
import com.florez.backend.modelregistry.domain.model.ModelActivation;
import com.florez.backend.modelregistry.domain.model.ModelArtifact;
import com.florez.backend.modelregistry.domain.model.ModelVersion;
import com.florez.backend.modelregistry.domain.port.out.ModelActivationRepositoryPort;
import com.florez.backend.modelregistry.domain.port.out.ModelArtifactCatalogPort;
import com.florez.backend.modelregistry.domain.port.out.ModelVersionRepositoryPort;
import com.florez.backend.scoring.domain.model.ModelFeatures;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ModelRegistryServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-05T09:00:00Z");

    @Mock
    private ModelVersionRepositoryPort versionRepository;

    @Mock
    private ModelActivationRepositoryPort activationRepository;

    @Mock
    private ModelArtifactCatalogPort catalog;

    private ModelRegistryService service;

    @BeforeEach
    void setUp() {
        service = new ModelRegistryService(versionRepository, activationRepository, catalog,
                Set.copyOf(ModelFeatures.FEATURE_NAMES), Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static ModelVersion registered(String version) {
        return new ModelVersion(ModelRegistryTestData.artifact(version), "admin", NOW.minusSeconds(60));
    }

    @Test
    void register_tomaLaMetadataDelServicioDeMlYLaGuarda() {
        when(versionRepository.exists("v1")).thenReturn(false);
        when(catalog.find("v1")).thenReturn(Optional.of(new CatalogEntry(ModelRegistryTestData.artifact("v1"), true)));
        when(versionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ModelVersion result = service.register("v1", "admin");

        assertThat(result.artifact()).isEqualTo(ModelRegistryTestData.artifact("v1"));
        assertThat(result.registeredBy()).isEqualTo("admin");
        assertThat(result.registeredAt()).isEqualTo(NOW);
    }

    @Test
    void register_versionYaRegistrada_lanzaDuplicate() {
        when(versionRepository.exists("v1")).thenReturn(true);

        assertThatThrownBy(() -> service.register("v1", "admin")).isInstanceOf(DuplicateResourceException.class);
        verify(catalog, never()).find(any());
    }

    @Test
    void register_versionQueElMlNoTiene_lanzaNotFound() {
        when(catalog.find("v9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register("v9", "admin")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void register_artefactoAlteradoEnDisco_seRechaza() {
        when(catalog.find("v1")).thenReturn(Optional.of(new CatalogEntry(ModelRegistryTestData.artifact("v1"), false)));

        assertThatThrownBy(() -> service.register("v1", "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("fue modificado");
        verify(versionRepository, never()).save(any());
    }

    @Test
    void register_modeloQueEsperaVariablesQueElBackendNoCalcula_seRechaza() {
        ModelArtifact base = ModelRegistryTestData.artifact("v1");
        ModelArtifact otherFeatures = new ModelArtifact(base.version(), base.algorithm(), base.hyperparameters(),
                base.trainedAt(), base.dataset(), base.metrics(), List.of("age", "credit_score_bureau"),
                base.artifactSha256());
        when(catalog.find("v1")).thenReturn(Optional.of(new CatalogEntry(otherFeatures, true)));

        assertThatThrownBy(() -> service.register("v1", "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("credit_score_bureau");
        verify(versionRepository, never()).save(any());
    }

    @Test
    void activate_conArtefactoIdenticoAlRegistrado_registraLaActivacionConSuMotivo() {
        when(versionRepository.findByVersion("v2")).thenReturn(Optional.of(registered("v2")));
        when(activationRepository.findCurrent())
                .thenReturn(Optional.of(new ModelActivation(1L, "v1", "inicial", "admin", NOW.minusSeconds(3600))));
        when(catalog.find("v2")).thenReturn(Optional.of(new CatalogEntry(ModelRegistryTestData.artifact("v2"), true)));
        when(activationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.activate("v2", "  mejor AUC en validación  ", "admin");

        ArgumentCaptor<ModelActivation> saved = ArgumentCaptor.forClass(ModelActivation.class);
        verify(activationRepository).save(saved.capture());
        assertThat(saved.getValue().modelVersion()).isEqualTo("v2");
        assertThat(saved.getValue().reason()).isEqualTo("mejor AUC en validación");
        assertThat(saved.getValue().activatedBy()).isEqualTo("admin");
        assertThat(saved.getValue().activatedAt()).isEqualTo(NOW);
    }

    @Test
    void activate_versionNoRegistrada_lanzaNotFound() {
        when(versionRepository.findByVersion("v9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate("v9", "motivo", "admin")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void activate_versionQueYaEstaActiva_seRechaza() {
        when(versionRepository.findByVersion("v1")).thenReturn(Optional.of(registered("v1")));
        when(activationRepository.findCurrent())
                .thenReturn(Optional.of(new ModelActivation(1L, "v1", "inicial", "admin", NOW.minusSeconds(3600))));

        assertThatThrownBy(() -> service.activate("v1", "motivo", "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ya es la versión activa");
        verify(activationRepository, never()).save(any());
    }

    @Test
    void activate_siElMlSirveUnArtefactoDistintoAlRegistrado_seRechaza() {
        when(versionRepository.findByVersion("v1")).thenReturn(Optional.of(registered("v1")));
        when(activationRepository.findCurrent()).thenReturn(Optional.empty());
        ModelArtifact replaced = ModelRegistryTestData.artifact("v1", "c".repeat(64));
        when(catalog.find("v1")).thenReturn(Optional.of(new CatalogEntry(replaced, true)));

        assertThatThrownBy(() -> service.activate("v1", "motivo", "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no coincide con el registrado");
        verify(activationRepository, never()).save(any());
    }

    @Test
    void activate_siElMlYaNoTieneElArtefacto_seRechaza() {
        when(versionRepository.findByVersion("v1")).thenReturn(Optional.of(registered("v1")));
        when(activationRepository.findCurrent()).thenReturn(Optional.empty());
        when(catalog.find("v1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activate("v1", "motivo", "admin"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ya no tiene el artefacto");
    }
}
