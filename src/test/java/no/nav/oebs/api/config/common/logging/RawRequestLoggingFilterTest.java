package no.nav.oebs.api.config.common.logging;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RawRequestLoggingFilterTest {

    @Test
    void filter_truncatesLargeBodyAndResetsEntityStream() {
        RawRequestLoggingFilter filter = new RawRequestLoggingFilter();
        ContainerRequestContext ctx = mock(ContainerRequestContext.class);
        UriInfo uriInfo = mock(UriInfo.class);

        String longBody = "a".repeat(10_050);
        when(ctx.getMethod()).thenReturn("POST");
        when(ctx.hasEntity()).thenReturn(true);
        when(ctx.getEntityStream()).thenReturn(new ByteArrayInputStream(longBody.getBytes(StandardCharsets.UTF_8)));
        when(ctx.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("https://example.test/scim/v2/Users"));

        assertDoesNotThrow(() -> filter.filter(ctx));
        verify(ctx).setEntityStream(any(ByteArrayInputStream.class));
    }

    @Test
    void sanitizeForLog_returnsNull_whenInputNull() {
        RawRequestLoggingFilter filter = new RawRequestLoggingFilter();

        String result = ReflectionTestUtils.invokeMethod(filter, "sanitizeForLog", (String) null);

        assertThat(result).isNull();
    }
}

