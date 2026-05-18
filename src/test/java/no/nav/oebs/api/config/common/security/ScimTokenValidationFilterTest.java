package no.nav.oebs.api.config.common.security;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;
import no.nav.oebs.api.scim.KallLoggHelper;
import no.nav.security.token.support.core.configuration.IssuerConfiguration;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.jwt.JwtToken;
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.core.validation.JwtTokenValidationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;

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

    private ScimTokenValidationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ScimTokenValidationFilter(kallLoggHelper, validationHandler, multiIssuerConfiguration);
    }

    @Test
    void filter_allowsUnprotectedMetadataEndpoints_withoutTokenValidation() {
        when(requestContext.getMethod()).thenReturn("GET");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("Schemas");

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

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(requestContext).abortWith(any());
        verify(validationHandler, never()).getValidatedTokens(any());
    }

    @Test
    void filter_rejectsBearerToken_whenNoIssuerHasValidToken() {
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("Users");
        when(requestContext.getHeaderString("Authorization")).thenReturn("Bearer not-a-jwt");
        when(validationHandler.getValidatedTokens(any())).thenReturn(new TokenValidationContext(Map.of()));

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(kallLoggHelper).loggInn(any(), any(), any(Integer.class), any(Long.class), any(), contains("reasonCode=INVALID_JWT_FORMAT"));
        verify(requestContext).abortWith(any());
    }

    @Test
    void filter_rejectsBearerToken_whenTokenParsingThrowsRuntimeException() {
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("Users");
        when(requestContext.getHeaderString("Authorization")).thenReturn("Bearer aa.b!.cc");
        when(validationHandler.getValidatedTokens(any())).thenReturn(new TokenValidationContext(Map.of()));

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(kallLoggHelper).loggInn(any(), any(), any(Integer.class), any(Long.class), any(), contains("reasonCode=INVALID_TOKEN"));
        verify(requestContext).abortWith(any());
    }

    @Test
    void filter_allowsRequest_whenAnyIssuerHasValidToken() {
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("Users");
        when(requestContext.getHeaderString("Authorization")).thenReturn("Bearer whatever");

        JwtToken jwtToken = mock(JwtToken.class);
        when(validationHandler.getValidatedTokens(any())).thenReturn(new TokenValidationContext(Map.of("issuer-a", jwtToken)));

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(requestContext, never()).abortWith(any());
        verify(kallLoggHelper, never()).loggInn(any(), any(), any(Integer.class), any(Long.class), any(), any());
    }

    @Test
    void filter_passesAuthorizationHeaderToValidationHandler_andReturnsNullForOtherHeaders() {
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("Users");
        when(requestContext.getHeaderString("Authorization")).thenReturn("Bearer token");

        when(validationHandler.getValidatedTokens(any())).thenAnswer(invocation -> {
            no.nav.security.token.support.core.http.HttpRequest request = invocation.getArgument(0);
            assertEquals("Bearer token", request.getHeader("Authorization"));
            assertEquals(null, request.getHeader("X-Other"));
            return new TokenValidationContext(Map.of("issuer-a", mock(JwtToken.class)));
        });

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void filter_rejectsBearerToken_whenIssuerPresentButJwtTokenIsNull() {
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("Users");
        when(requestContext.getHeaderString("Authorization")).thenReturn("Bearer not-a-jwt");

        TokenValidationContext tokenContext = mock(TokenValidationContext.class);
        when(tokenContext.getIssuers()).thenReturn(List.of("issuer-a"));
        when(tokenContext.getJwtToken("issuer-a")).thenReturn(null);
        when(validationHandler.getValidatedTokens(any())).thenReturn(tokenContext);

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(kallLoggHelper).loggInn(any(), any(), eq(401), eq(0L), any(), contains("reasonCode=INVALID_JWT_FORMAT"));
    }

    @ParameterizedTest
    @CsvSource({
            "GET,Schemas,true",
            "GET,/Schemas,true",
            "GET,ResourceTypes,true",
            "GET,/ResourceTypes,true",
            "GET,ServiceProviderConfig,true",
            "GET,/ServiceProviderConfig,true",
            "POST,Schemas,false",
            "GET,Users,false"
    })
    void isUnprotected_coversAllPathVariants(String method, String path, boolean expected) throws Exception {
        Method isUnprotected = ScimTokenValidationFilter.class.getDeclaredMethod("isUnprotected", String.class, String.class);
        isUnprotected.setAccessible(true);

        boolean actual = (boolean) isUnprotected.invoke(filter, method, path);
        assertEquals(expected, actual);
    }

    @Test
    void sanitizeForLog_handlesNullAndControlCharacters() throws Exception {
        Method sanitize = ScimTokenValidationFilter.class.getDeclaredMethod("sanitizeForLog", String.class);
        sanitize.setAccessible(true);

        assertEquals(null, sanitize.invoke(filter, (Object) null));
        assertEquals("a\\rb\\nc\\t_", sanitize.invoke(filter, "a\rb\nc\t\u0001"));
    }

    @Test
    void klassifiserAvvisningsgrunn_returnsUnknown_forNullAndBlankReason() throws Exception {
        Method klassifiser = ScimTokenValidationFilter.class.getDeclaredMethod("klassifiserAvvisningsgrunn", String.class);
        klassifiser.setAccessible(true);

        assertEquals("UNKNOWN", klassifiser.invoke(filter, (Object) null));
        assertEquals("UNKNOWN", klassifiser.invoke(filter, "  "));
    }

    @Test
    void filter_rejectsExpiredToken_withTokenExpiredReasonCode() {
        setUpInvalidTokenRequest(withBearerPayload("{\"exp\":1,\"iss\":\"https://issuer\",\"aud\":\"aud-1\"}"));

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(kallLoggHelper).loggInn(any(), any(), any(Integer.class), any(Long.class), any(), contains("reasonCode=TOKEN_EXPIRED"));
    }

    @Test
    void filter_rejectsTokenMissingExp_withMissingExpReasonCode() {
        setUpInvalidTokenRequest(withBearerPayload("{\"iss\":\"https://issuer\",\"aud\":\"aud-1\"}"));

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(kallLoggHelper).loggInn(any(), any(), any(Integer.class), any(Long.class), any(), contains("reasonCode=MISSING_EXP"));
    }

    @Test
    void filter_rejectsUnknownIssuer_withUnknownIssuerReasonCode() {
        setUpInvalidTokenRequest(withBearerPayload("{\"exp\":4102444800,\"iss\":\"https://unknown\",\"aud\":\"aud-1\"}"));

        IssuerConfiguration issuerConfig = mock(IssuerConfiguration.class, RETURNS_DEEP_STUBS);
        when(issuerConfig.getMetadata().getIssuer().getValue()).thenReturn("https://known");
        when(issuerConfig.getAcceptedAudience()).thenReturn(List.of("aud-1"));
        when(multiIssuerConfiguration.getIssuers()).thenReturn(Map.of("known", issuerConfig));
        when(multiIssuerConfiguration.getIssuerShortNames()).thenReturn(new java.util.ArrayList<>(List.of("known")));

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(kallLoggHelper).loggInn(any(), any(), any(Integer.class), any(Long.class), any(), contains("reasonCode=UNKNOWN_ISSUER"));
    }

    @Test
    void filter_rejectsInvalidAudience_withInvalidAudienceReasonCode() {
        setUpInvalidTokenRequest(withBearerPayload("{\"exp\":4102444800,\"iss\":\"https://known\",\"aud\":\"aud-x\"}"));

        IssuerConfiguration issuerConfig = mock(IssuerConfiguration.class, RETURNS_DEEP_STUBS);
        when(issuerConfig.getMetadata().getIssuer().getValue()).thenReturn("https://known");
        when(issuerConfig.getAcceptedAudience()).thenReturn(List.of("aud-ok"));
        when(multiIssuerConfiguration.getIssuers()).thenReturn(Map.of("known", issuerConfig));

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(kallLoggHelper).loggInn(any(), any(), any(Integer.class), any(Long.class), any(), contains("reasonCode=INVALID_AUDIENCE"));
    }

    @Test
    void filter_rejectsTokenWithValidIssuerAndAudience_withFallbackInvalidTokenReasonCode() {
        setUpInvalidTokenRequest(withBearerPayload("{\"exp\":4102444800,\"iss\":\"https://known\",\"aud\":\"aud-ok\"}"));

        IssuerConfiguration issuerConfig = mock(IssuerConfiguration.class, RETURNS_DEEP_STUBS);
        when(issuerConfig.getMetadata().getIssuer().getValue()).thenReturn("https://known");
        when(issuerConfig.getAcceptedAudience()).thenReturn(List.of("aud-ok"));
        when(multiIssuerConfiguration.getIssuers()).thenReturn(Map.of("known", issuerConfig));

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(kallLoggHelper).loggInn(any(), any(), any(Integer.class), any(Long.class), any(), contains("reasonCode=INVALID_TOKEN"));
    }

    @Test
    void filter_rejectsTokenWithMissingIssAndAud_withUnknownIssuerReasonCode() {
        setUpInvalidTokenRequest(withBearerPayload("{\"exp\":4102444800}"));
        when(multiIssuerConfiguration.getIssuers()).thenReturn(Map.of());
        when(multiIssuerConfiguration.getIssuerShortNames()).thenReturn(new java.util.ArrayList<>());

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(kallLoggHelper).loggInn(any(), any(), any(Integer.class), any(Long.class), any(), contains("reasonCode=UNKNOWN_ISSUER"));
    }

    @Test
    void filter_rejectsToken_whenIssuerMetadataThrowsRuntimeException() {
        setUpInvalidTokenRequest(withBearerPayload("{\"exp\":4102444800,\"iss\":\"https://known\",\"aud\":\"aud-1\"}"));

        IssuerConfiguration issuerConfig = mock(IssuerConfiguration.class);
        when(issuerConfig.getMetadata()).thenThrow(new RuntimeException("metadata unavailable"));
        when(multiIssuerConfiguration.getIssuers()).thenReturn(Map.of("known", issuerConfig));
        when(multiIssuerConfiguration.getIssuerShortNames()).thenReturn(new java.util.ArrayList<>(List.of("known")));

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(kallLoggHelper).loggInn(any(), any(), any(Integer.class), any(Long.class), any(), contains("reasonCode=UNKNOWN_ISSUER"));
    }

    @Test
    void filter_rejectsToken_whenIssuerMetadataValueIsNull() {
        setUpInvalidTokenRequest(withBearerPayload("{\"exp\":4102444800,\"iss\":\"https://known\",\"aud\":\"aud-1\"}"));

        IssuerConfiguration issuerConfig = mock(IssuerConfiguration.class, RETURNS_DEEP_STUBS);
        when(issuerConfig.getMetadata().getIssuer().getValue()).thenReturn(null);
        when(multiIssuerConfiguration.getIssuers()).thenReturn(Map.of("known", issuerConfig));
        when(multiIssuerConfiguration.getIssuerShortNames()).thenReturn(new java.util.ArrayList<>(List.of("known")));

        assertDoesNotThrow(() -> filter.filter(requestContext));
        verify(kallLoggHelper).loggInn(any(), any(), any(Integer.class), any(Long.class), any(), contains("reasonCode=UNKNOWN_ISSUER"));
    }

    private void setUpInvalidTokenRequest(String authHeader) {
        when(requestContext.getMethod()).thenReturn("POST");
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getPath()).thenReturn("Users");
        when(requestContext.getHeaderString("Authorization")).thenReturn(authHeader);
        when(validationHandler.getValidatedTokens(any())).thenReturn(new TokenValidationContext(Map.of()));
    }

    private static String withBearerPayload(String payloadJson) {
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        return "Bearer header." + payload + ".signature";
    }
}

