package no.nav.oebs.api.config.common.logging;

import jakarta.servlet.FilterChain;
import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.KallLoggRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HttpLoggingFilterTest {

    @Mock
    private KallLoggRepository kallLoggRepository;

    @Test
    void doFilterInternal_savesKallLogg() throws Exception {
        HttpLoggingFilter filter = new HttpLoggingFilter(kallLoggRepository);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/scim/v2/Users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<KallLogg> captor = ArgumentCaptor.forClass(KallLogg.class);
        verify(kallLoggRepository).save(captor.capture());
        assertThat(captor.getValue().getMethod()).isEqualTo("GET");
        assertThat(captor.getValue().getOperation()).isEqualTo("/scim/v2/Users");
    }

    @Test
    void doFilterInternal_doesNotThrow_whenSaveFails() {
        HttpLoggingFilter filter = new HttpLoggingFilter(kallLoggRepository);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/scim/v2/Users");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };
        doThrow(new RuntimeException("db down")).when(kallLoggRepository).save(org.mockito.ArgumentMatchers.any(KallLogg.class));

        assertDoesNotThrow(() -> filter.doFilterInternal(request, response, chain));
    }
}

