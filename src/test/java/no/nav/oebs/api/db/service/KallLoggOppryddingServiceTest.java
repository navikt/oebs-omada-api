package no.nav.oebs.api.db.service;

import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.KallLoggRepository;
import no.nav.oebs.api.scim.KallLoggHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KallLoggOppryddingServiceTest {

    @Mock
    private KallLoggRepository kallLoggRepository;

    @Mock
    private KallLoggHelper kallLoggHelper;

    @InjectMocks
    private KallLoggOppryddingService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "retentionDays", 30);
    }

    @Test
    void ryddOppGamleRader_logsAndReturns_whenNothingToDelete() {
        when(kallLoggRepository.count()).thenReturn(10L);
        when(kallLoggRepository.tellGamleRader(any(LocalDateTime.class))).thenReturn(0L);

        service.ryddOppGamleRader();

        verify(kallLoggRepository, never()).slettGamleRader(any(LocalDateTime.class));
        verify(kallLoggHelper).loggUt(eq(KallLogg.METHOD_DELETE), contains("XXRTV_OMADA_LOG opprydding"),
                eq(200), eq(0L), contains("\"skalSlettes\":0"), contains("\"slettet\":0"), eq(null));
    }

    @Test
    void ryddOppGamleRader_logsSuccess_whenRowsAreDeleted() {
        when(kallLoggRepository.count()).thenReturn(20L, 15L);
        when(kallLoggRepository.tellGamleRader(any(LocalDateTime.class))).thenReturn(5L);
        when(kallLoggRepository.slettGamleRader(any(LocalDateTime.class))).thenReturn(5);

        service.ryddOppGamleRader();

        verify(kallLoggRepository).slettGamleRader(any(LocalDateTime.class));
        verify(kallLoggHelper).loggUt(eq(KallLogg.METHOD_DELETE), contains("XXRTV_OMADA_LOG opprydding"),
                eq(200), any(Long.class), contains("\"skalSlettes\":5"), contains("\"slettet\":5"), eq(null));
    }

    @Test
    void ryddOppGamleRader_logsError_whenDeleteFails() {
        when(kallLoggRepository.count()).thenReturn(20L);
        when(kallLoggRepository.tellGamleRader(any(LocalDateTime.class))).thenReturn(5L);
        when(kallLoggRepository.slettGamleRader(any(LocalDateTime.class))).thenThrow(new RuntimeException("db down"));

        service.ryddOppGamleRader();

        verify(kallLoggHelper).loggUt(eq(KallLogg.METHOD_DELETE), contains("XXRTV_OMADA_LOG opprydding"),
                eq(500), any(Long.class), contains("\"skalSlettes\":5"), eq(null), contains("db down"));
    }
}

