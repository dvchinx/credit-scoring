package com.florez.backend.creditapplication.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.florez.backend.common.domain.DuplicateResourceException;
import com.florez.backend.common.domain.ResourceNotFoundException;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.model.CreditHistory;
import com.florez.backend.creditapplication.domain.port.out.CreditApplicationRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditApplicationServiceTest {

    @Mock
    private CreditApplicationRepositoryPort repositoryPort;

    private CreditApplicationService service;

    @BeforeEach
    void setUp() {
        service = new CreditApplicationService(repositoryPort);
    }

    private CreditApplication newApplication(String documentId) {
        return CreditApplication.createNew(
                "Jane Doe",
                documentId,
                LocalDate.of(1990, 1, 1),
                new BigDecimal("3000.00"),
                new BigDecimal("10000.00"),
                24,
                5.0,
                new BigDecimal("500.00"),
                1,
                new CreditHistory(0.25, 4, 1, 0, 0, 0),
                "analyst1");
    }

    @Test
    void create_conDocumentIdNuevo_persisteLaSolicitud() {
        CreditApplication toCreate = newApplication("DOC-1");
        when(repositoryPort.existsByDocumentId("DOC-1")).thenReturn(false);
        when(repositoryPort.save(toCreate)).thenReturn(toCreate);

        CreditApplication created = service.create(toCreate);

        assertThat(created.getDocumentId()).isEqualTo("DOC-1");
        verify(repositoryPort).save(toCreate);
    }

    @Test
    void create_conDocumentIdDuplicado_lanzaDuplicateResource() {
        CreditApplication toCreate = newApplication("DOC-1");
        when(repositoryPort.existsByDocumentId("DOC-1")).thenReturn(true);

        assertThatThrownBy(() -> service.create(toCreate)).isInstanceOf(DuplicateResourceException.class);
        verify(repositoryPort, never()).save(any());
    }

    @Test
    void getById_conIdInexistente_lanzaResourceNotFound() {
        UUID id = UUID.randomUUID();
        when(repositoryPort.findByIdAndNotDeleted(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_marcaLaSolicitudComoEliminada() {
        UUID id = UUID.randomUUID();
        CreditApplication existing = newApplication("DOC-1");
        when(repositoryPort.findByIdAndNotDeleted(id)).thenReturn(Optional.of(existing));
        when(repositoryPort.save(existing)).thenReturn(existing);

        service.delete(id);

        assertThat(existing.isDeleted()).isTrue();
        verify(repositoryPort, times(1)).save(existing);
    }

    @Test
    void update_conDocumentIdYaUsadoPorOtraSolicitud_lanzaDuplicateResource() {
        UUID id = UUID.randomUUID();
        CreditApplication existing = newApplication("DOC-1");
        CreditApplication updateData = newApplication("DOC-2");
        when(repositoryPort.findByIdAndNotDeleted(id)).thenReturn(Optional.of(existing));
        when(repositoryPort.existsByDocumentId("DOC-2")).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, updateData)).isInstanceOf(DuplicateResourceException.class);
        verify(repositoryPort, never()).save(any());
    }
}
