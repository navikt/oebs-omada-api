package no.nav.oebs.api.db.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import no.nav.oebs.api.db.entity.KallLogg;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KallLoggRepositoryImplTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private TypedQuery<KallLogg> kallLoggQuery;

    @Mock
    private Query deleteQuery;

    @Mock
    private TypedQuery<Long> countQuery;

    private KallLoggRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new KallLoggRepositoryImpl();
        ReflectionTestUtils.setField(repository, "entityManager", entityManager);
    }

    @Test
    void pingKallLogg_executesExpectedSelect() {
        when(entityManager.createQuery("SELECT k FROM KallLogg k WHERE k.id = 0", KallLogg.class)).thenReturn(kallLoggQuery);
        when(kallLoggQuery.getResultList()).thenReturn(List.of());

        repository.pingKallLogg();

        verify(entityManager).createQuery("SELECT k FROM KallLogg k WHERE k.id = 0", KallLogg.class);
        verify(kallLoggQuery).getResultList();
    }

    @Test
    void slettGamleRader_deletesRowsOlderThanThreshold() {
        LocalDateTime grense = LocalDateTime.now().minusDays(1);
        when(entityManager.createQuery("DELETE FROM KallLogg k WHERE k.tidspunkt < :grense")).thenReturn(deleteQuery);
        when(deleteQuery.setParameter("grense", grense)).thenReturn(deleteQuery);
        when(deleteQuery.executeUpdate()).thenReturn(7);

        int deleted = repository.slettGamleRader(grense);

        assertThat(deleted).isEqualTo(7);
        verify(deleteQuery).setParameter("grense", grense);
        verify(deleteQuery).executeUpdate();
    }

    @Test
    void tellGamleRader_countsRowsOlderThanThreshold() {
        LocalDateTime grense = LocalDateTime.now().minusDays(2);
        when(entityManager.createQuery("SELECT COUNT(k) FROM KallLogg k WHERE k.tidspunkt < :grense", Long.class)).thenReturn(countQuery);
        when(countQuery.setParameter("grense", grense)).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(12L);

        long count = repository.tellGamleRader(grense);

        assertThat(count).isEqualTo(12L);
        verify(countQuery).setParameter("grense", grense);
        verify(countQuery).getSingleResult();
    }

    @Test
    void slettAlleRader_deletesAllRows() {
        when(entityManager.createQuery("DELETE FROM KallLogg k")).thenReturn(deleteQuery);
        when(deleteQuery.executeUpdate()).thenReturn(3);

        int deleted = repository.slettAlleRader();

        assertThat(deleted).isEqualTo(3);
        verify(deleteQuery).executeUpdate();
    }
}

