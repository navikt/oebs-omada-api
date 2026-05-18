package no.nav.oebs.api.config.common.logging;

import jakarta.servlet.FilterChain;
import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.KallLoggRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;

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

    @Test
    void doFilterInternal_includesQueryStringInRequestLog() throws Exception {
        HttpLoggingFilter filter = new HttpLoggingFilter(kallLoggRepository);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/scim/v2/Users");
        request.setQueryString("startIndex=1&count=10");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<KallLogg> captor = ArgumentCaptor.forClass(KallLogg.class);
        verify(kallLoggRepository).save(captor.capture());
        assertThat(captor.getValue().getRequest()).contains("GET /scim/v2/Users?startIndex=1&count=10");
    }

    @Test
    void formatRequestBody_returnsEarly_whenRequestIsNotWrapped() {
        HttpLoggingFilter filter = new HttpLoggingFilter(kallLoggRepository);
        StringBuilder builder = new StringBuilder();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/x");

        ReflectionTestUtils.invokeMethod(filter, "formatBody", builder, request);

        assertThat(builder).hasToString("");
    }

    @Test
    void formatRequestBody_usesUnknownPayload_whenEncodingIsInvalid() throws Exception {
        HttpLoggingFilter filter = new HttpLoggingFilter(kallLoggRepository);
        StringBuilder builder = new StringBuilder();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/x");
        request.setContent("payload".getBytes(StandardCharsets.UTF_8));
        request.setCharacterEncoding("x-invalid-encoding");
        ContentCachingRequestWrapper wrapper = new ContentCachingRequestWrapper(request, 1024);
        wrapper.getInputStream().readAllBytes();

        ReflectionTestUtils.invokeMethod(filter, "formatBody", builder, wrapper);

        assertThat(builder).hasToString("[unknown]");
    }

    @Test
    void formatResponseBody_returnsEarly_whenResponseIsNotWrapped() {
        HttpLoggingFilter filter = new HttpLoggingFilter(kallLoggRepository);
        StringBuilder builder = new StringBuilder();
        MockHttpServletResponse response = new MockHttpServletResponse();

        ReflectionTestUtils.invokeMethod(filter, "formatBody", builder, response);

        assertThat(builder).hasToString("");
    }

    @Test
    void formatResponseBody_usesUnknownPayload_whenEncodingIsInvalid() throws Exception {
        HttpLoggingFilter filter = new HttpLoggingFilter(kallLoggRepository);
        StringBuilder builder = new StringBuilder();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setCharacterEncoding("x-invalid-encoding");
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        wrapper.getOutputStream().write("payload".getBytes(StandardCharsets.UTF_8));

        ReflectionTestUtils.invokeMethod(filter, "formatBody", builder, wrapper);

        assertThat(builder).hasToString("[unknown]");
    }

    @Test
    void formatResponseBody_copiesBodyToResponse_whenPayloadExists() throws Exception {
        HttpLoggingFilter filter = new HttpLoggingFilter(kallLoggRepository);
        StringBuilder builder = new StringBuilder();
        MockHttpServletResponse response = new MockHttpServletResponse();
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        wrapper.getOutputStream().write("ok".getBytes(StandardCharsets.UTF_8));

        ReflectionTestUtils.invokeMethod(filter, "formatBody", builder, wrapper);

        assertThat(builder).hasToString("ok");
        assertThat(response.getContentAsString()).isEqualTo("ok");
    }

    @Test
    void formatHeaders_joinsMultipleValuesWithComma() {
        HttpLoggingFilter filter = new HttpLoggingFilter(kallLoggRepository);
        StringBuilder builder = new StringBuilder();
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Test", "a");
        headers.add("X-Test", "b");

        ReflectionTestUtils.invokeMethod(filter, "formatHeaders", builder, headers);

        assertThat(builder.toString()).contains("X-Test: a, b");
    }
}

