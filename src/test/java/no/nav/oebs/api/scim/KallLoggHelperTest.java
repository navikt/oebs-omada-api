package no.nav.oebs.api.scim;

import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.KallLoggRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KallLoggHelperTest {

    @Mock
    private KallLoggRepository kallLoggRepository;

    @InjectMocks
    private KallLoggHelper helper;

    @Test
    void loggInn_savesKallLogg() {
        helper.loggInn("GET", "/scim/v2/Users", 200, 12L, "resp", null);

        ArgumentCaptor<KallLogg> captor = ArgumentCaptor.forClass(KallLogg.class);
        verify(kallLoggRepository).save(captor.capture());
        assertThat(captor.getValue().getMethod()).isEqualTo("GET");
        assertThat(captor.getValue().getOperation()).isEqualTo("/scim/v2/Users");
        assertThat(captor.getValue().getStatus()).isEqualTo(200);
    }

    @Test
    void loggUt_doesNotThrow_whenRepositoryFails() {
        doThrow(new RuntimeException("db down")).when(kallLoggRepository).save(org.mockito.ArgumentMatchers.any(KallLogg.class));

        assertDoesNotThrow(() -> helper.loggUt("POST", "proc", 500, 1L, "req", "resp", "info"));
    }
}

