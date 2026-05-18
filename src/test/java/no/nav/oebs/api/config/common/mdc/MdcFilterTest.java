package no.nav.oebs.api.config.common.mdc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MdcFilterTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private final MdcFilter filter = new MdcFilter();

    @Test
    void doFilterInternal_setsCorrelationId_callsChain_andCleansUp() throws ServletException, IOException {
        try (MockedStatic<MdcOperations> mdc = mockStatic(MdcOperations.class)) {
            mdc.when(MdcOperations::generateCorrelationId).thenReturn("cid-1");

            filter.doFilterInternal(request, response, filterChain);

            mdc.verify(MdcOperations::generateCorrelationId);
            mdc.verify(() -> MdcOperations.put(MdcOperations.MDC_CORRELATION_ID, "cid-1"));
            mdc.verify(() -> MdcOperations.remove(MdcOperations.MDC_CORRELATION_ID));
            verify(filterChain).doFilter(request, response);
        }
    }

    @Test
    void doFilterInternal_removesCorrelationId_evenWhenChainThrows() throws ServletException, IOException {
        doThrow(new IOException("boom")).when(filterChain).doFilter(request, response);

        try (MockedStatic<MdcOperations> mdc = mockStatic(MdcOperations.class)) {
            mdc.when(MdcOperations::generateCorrelationId).thenReturn("cid-2");

            assertThrows(IOException.class, () -> filter.doFilterInternal(request, response, filterChain));

            mdc.verify(() -> MdcOperations.remove(MdcOperations.MDC_CORRELATION_ID));
        }
    }
}

