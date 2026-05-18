package no.nav.oebs.api.config.common.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import no.nav.oebs.api.scim.KallLoggHelper;
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScimTokenValidationFilterTest {

    @Mock
    private KallLoggHelper kallLoggHelper;

    @Mock
    private JwtTokenValidationHandler validationHandler;

    @Mock
    private MultiIssuerConfiguration multiIssuerConfiguration;

    @Mock
    private ContainerRequestContext requestContext;

    @Mock
    private UriInfo uriInfo;

    @Test
    void filter_allowsUnprotectedMetadataEndpoints_withoutTokenValidation() {
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("Schemas");

        ScimTokenValidationFilter filter = new ScimTokenValidationFilter(kallLoggHelper, validationHandler, multiIssuerConfiguration);

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(validationHandler, never()).getValidatedTokens(any());
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void filter_rejectsMissingAuthorizationHeader() {
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("Users");
        when(requestContext.getHeaderString("Authorization")).thenReturn(null);

        ScimTokenValidationFilter filter = new ScimTokenValidationFilter(kallLoggHelper, validationHandler, multiIssuerConfiguration);

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(kallLoggHelper).loggInn(any(), any(), any(Integer.class), any(Long.class), any(), any());
        verify(requestContext).abortWith(any());
    }

    @Test
    void filter_rejectsNonBearerAuthorizationHeader() {
        when(requestContext.getMethod()).thenReturn("PUT");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("Users/K123");
        when(requestContext.getHeaderString("Authorization")).thenReturn("Basic abc");

        ScimTokenValidationFilter filter = new ScimTokenValidationFilter(kallLoggHelper, validationHandler, multiIssuerConfiguration);

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(requestContext).abortWith(any());
        verify(validationHandler, never()).getValidatedTokens(any());
    }
}

