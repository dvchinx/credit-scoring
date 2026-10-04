package com.florez.backend.creditapplication.infrastructure.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.florez.backend.TestcontainersConfig;
import com.florez.backend.creditapplication.domain.model.ApplicationStatus;
import com.florez.backend.creditapplication.domain.model.CreditApplication;
import com.florez.backend.creditapplication.domain.model.CreditHistory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfig.class)
class CreditApplicationRepositoryAdapterIT {

    @Autowired
    private CreditApplicationRepositoryAdapter adapter;

    private CreditApplication newApplication(String documentId) {
        return CreditApplication.createNew(
                "John Smith",
                documentId,
                LocalDate.of(1985, 5, 20),
                new BigDecimal("4000.00"),
                new BigDecimal("15000.00"),
                36,
                8.0,
                new BigDecimal("300.00"),
                2,
                new CreditHistory(0.25, 4, 1, 0, 0, 0),
                "analyst1");
    }

    @Test
    void save_asignaIdYPermiteRecuperarLaSolicitud() {
        CreditApplication saved = adapter.save(newApplication("IT-DOC-1"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.PENDING);

        Optional<CreditApplication> found = adapter.findByIdAndNotDeleted(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getDocumentId()).isEqualTo("IT-DOC-1");
    }

    @Test
    void save_conSoftDelete_dejaDeSerVisibleEnFindByIdAndNotDeleted() {
        CreditApplication saved = adapter.save(newApplication("IT-DOC-2"));
        saved.softDelete();
        adapter.save(saved);

        Optional<CreditApplication> found = adapter.findByIdAndNotDeleted(saved.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdAndNotDeleted_conIdInexistente_devuelveVacio() {
        assertThat(adapter.findByIdAndNotDeleted(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findAllNotDeleted_excluyeLasEliminadas() {
        CreditApplication visible = adapter.save(newApplication("IT-DOC-3"));
        CreditApplication deleted = adapter.save(newApplication("IT-DOC-4"));
        deleted.softDelete();
        adapter.save(deleted);

        var page = adapter.findAllNotDeleted(PageRequest.of(0, 50));

        assertThat(page.getContent())
                .extracting(CreditApplication::getDocumentId)
                .contains(visible.getDocumentId())
                .doesNotContain(deleted.getDocumentId());
    }

    @Test
    void existsByDocumentId_conDocumentoYaUsado_devuelveTrue() {
        adapter.save(newApplication("IT-DOC-5"));

        assertThat(adapter.existsByDocumentId("IT-DOC-5")).isTrue();
        assertThat(adapter.existsByDocumentId("IT-DOC-NOPE")).isFalse();
    }
}
