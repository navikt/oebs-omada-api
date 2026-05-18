package no.nav.oebs.api.scim.provider;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.PlsqlProcedureRepository;
import no.nav.oebs.api.db.repository.PlsqlProcedureResult;
import no.nav.oebs.api.scim.KallLoggHelper;
import no.nav.oebs.api.scim.service.ScimUserService;
import org.apache.directory.scim.spec.exception.ResourceException;
import org.apache.directory.scim.spec.filter.FilterResponse;
import org.apache.directory.scim.spec.filter.PageRequest;
import org.apache.directory.scim.spec.resources.ScimUser;
import org.apache.directory.scim.spec.schema.Meta;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScimUserResourceProviderCoverageTest {

    @Mock
    private ScimUserService userService;

    @Mock
    private PlsqlProcedureRepository plsqlRepository;

    @Mock
    private KallLoggHelper kallLoggHelper;

    @InjectMocks
    private ScimUserResourceProvider provider;

    @Test
    void getResourceClass_returnsScimUserClass() {
        assertThat(provider.getResourceClass()).isEqualTo(ScimUser.class);
    }

    @Test
    void getExtensionList_containsExpectedScimExtensions() {
        assertThat(provider.getExtensionList())
                .containsExactly(
                        no.nav.oebs.api.scim.extension.NavOebsExtension.class,
                        org.apache.directory.scim.spec.extension.EnterpriseExtension.class
                );
    }

    @Test
    void find_usesProvidedStartIndexAndCount() {
        PageRequest pageRequest = mock(PageRequest.class);
        when(pageRequest.getStartIndex()).thenReturn(7);
        when(pageRequest.getCount()).thenReturn(3);
        Page<ScimUser> page = new PageImpl<>(List.of(new ScimUser()));
        when(userService.getUsers(7, 3)).thenReturn(page);

        FilterResponse<ScimUser> response = provider.find(null, pageRequest, null);

        assertThat(response.getResources()).hasSize(1);
        verify(userService).getUsers(7, 3);
    }

    @Test
    void find_usesDefaultsWhenPageRequestFieldsAreNull() {
        PageRequest pageRequest = mock(PageRequest.class);
        when(pageRequest.getStartIndex()).thenReturn(null);
        when(pageRequest.getCount()).thenReturn(null);
        Page<ScimUser> page = new PageImpl<>(List.of(new ScimUser()));
        when(userService.getUsers(1, 100)).thenReturn(page);

        provider.find(null, pageRequest, null);

        verify(userService).getUsers(1, 100);
    }

    @Test
    void patch_logsAndThrows501() {
        ResourceException ex = assertThrows(ResourceException.class,
                () -> provider.patch("K1", null, List.of(), null, null));

        assertThat(ex.getStatus()).isEqualTo(501);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_PATCH), eq("/scim/v2/Users/K1"), eq(501), anyLong(), eq(null), anyString(), eq(null));
    }

    @Test
    void create_skipsSyncWhenSyncDisabled_evenWithInterfaceMsgId() throws ResourceException {
        ReflectionTestUtils.setField(provider, "syncEnabled", false);

        ScimUser resource = user("K_SYNC_OFF");
        ScimUser fromView = user("K_SYNC_OFF");
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.NY), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, 0, "ok", 42L, "0"));
        when(userService.getUser("K_SYNC_OFF")).thenReturn(Optional.of(fromView));

        ScimUser result = provider.create(resource);

        assertThat(result).isSameAs(fromView);
        verify(plsqlRepository, never()).executeSyncProcedure(any(), any());
    }

    @Test
    void create_syncSuccess_logs201ForCreate() throws ResourceException {
        ReflectionTestUtils.setField(provider, "syncEnabled", true);

        ScimUser resource = user("K_SYNC_OK");
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.NY), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, 0, "ok", 101L, "0"));
        when(plsqlRepository.executeSyncProcedure(any(), eq(101L)))
                .thenReturn(new PlsqlProcedureResult(null, 0, "sync ok", null, "0", "COMPLETE", "NORMAL"));
        when(userService.getUser("K_SYNC_OK")).thenReturn(Optional.of(resource));

        ScimUser result = provider.create(resource);

        assertThat(result).isSameAs(resource);
        verify(kallLoggHelper).loggUt(eq(KallLogg.METHOD_POST), any(), eq(201), anyLong(), eq("101"), anyString(), eq("sync ok"));
    }

    @Test
    void create_syncNegativeResultWithRetcode1_throws422() {
        ReflectionTestUtils.setField(provider, "syncEnabled", true);

        ScimUser resource = user("K_SYNC_422");
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.NY), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, 0, "ok", 202L, "0"));
        when(plsqlRepository.executeSyncProcedure(any(), eq(202L)))
                .thenReturn(new PlsqlProcedureResult(null, -1, "warning", null, "1", "COMPLETE", "NORMAL"));

        ResourceException ex = assertThrows(ResourceException.class, () -> provider.create(resource));

        assertThat(ex.getStatus()).isEqualTo(422);
        assertThat(ex.getMessage()).contains("retcode=1");
    }

    @Test
    void create_syncNegativeResultWithMissingErrbuf_throws500WithDefaultMessage() {
        ReflectionTestUtils.setField(provider, "syncEnabled", true);

        ScimUser resource = user("K_SYNC_500");
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.NY), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, 0, "ok", 303L, "0"));
        when(plsqlRepository.executeSyncProcedure(any(), eq(303L)))
                .thenReturn(new PlsqlProcedureResult(null, -1, null, null, "2", "COMPLETE", "NORMAL"));

        ResourceException ex = assertThrows(ResourceException.class, () -> provider.create(resource));

        assertThat(ex.getStatus()).isEqualTo(500);
        assertThat(ex.getMessage()).contains("Synkronisering feilet uten feilmelding");
    }

    @Test
    void create_syncMessageNumberPositive_onlyWarnsAndContinues() throws ResourceException {
        ReflectionTestUtils.setField(provider, "syncEnabled", true);

        ScimUser resource = user("K_SYNC_WARN");
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.NY), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, 0, "ok", 404L, "0"));
        when(plsqlRepository.executeSyncProcedure(any(), eq(404L)))
                .thenReturn(new PlsqlProcedureResult(null, 1, "warn", null, "0", "COMPLETE", "NORMAL"));
        when(userService.getUser("K_SYNC_WARN")).thenReturn(Optional.of(resource));

        ScimUser result = provider.create(resource);

        assertThat(result).isSameAs(resource);
    }

    @Test
    void create_warnsButDoesNotThrow_whenInsertMessageNumberPositive() throws ResourceException {
        ScimUser resource = user("K_WARN");
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.NY), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, 1, "insert warning", null, "0"));
        when(userService.getUser("K_WARN")).thenReturn(Optional.of(resource));

        ScimUser result = provider.create(resource);

        assertThat(result).isSameAs(resource);
    }

    @Test
    void create_throws400_whenIdBlankAndExternalIdBlank() {
        ScimUser resource = user(" ");
        resource.setExternalId("  ");

        ResourceException ex = assertThrows(ResourceException.class, () -> provider.create(resource));

        assertThat(ex.getStatus()).isEqualTo(400);
    }

    @Test
    void create_negativeInsertResult_usesNumericRetcodeAsHttpStatus() {
        ScimUser resource = user("K_404");
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.NY), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, -1, "not found", null, "404"));

        ResourceException ex = assertThrows(ResourceException.class, () -> provider.create(resource));

        assertThat(ex.getStatus()).isEqualTo(404);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_POST), eq("/scim/v2/Users"), eq(500), anyLong(), anyString(), eq(null), eq(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOT_NUMERIC", "700", "199"})
    void create_negativeInsertResult_withInvalidOrOutOfRangeRetcodeDefaultsTo500(String retcode) {
        ScimUser resource = user("K_500");
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.NY), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, -1, "error", null, retcode));

        ResourceException ex = assertThrows(ResourceException.class, () -> provider.create(resource));

        assertThat(ex.getStatus()).isEqualTo(500);
    }

    @Test
    void create_fallbackReusesExistingMetaInstance() throws ResourceException {
        ScimUser resource = user("K_META");
        Meta meta = new Meta();
        meta.setResourceType("User");
        resource.setMeta(meta);

        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.NY), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, 0, "ok", null, "0"));
        when(userService.getUser("K_META")).thenReturn(Optional.empty());

        ScimUser result = provider.create(resource);

        assertThat(result.getMeta()).isSameAs(meta);
        assertThat(result.getMeta().getVersion()).startsWith("W/\"");
    }

    @Test
    void create_fallbackCreatesMeta_whenMetaIsNull() throws ResourceException {
        ScimUser resource = user("K_NULL_META");
        resource.setMeta(null);

        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.NY), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, 0, "ok", null, "0"));
        when(userService.getUser("K_NULL_META")).thenReturn(Optional.empty());

        ScimUser result = provider.create(resource);

        assertThat(result.getMeta()).isNotNull();
        assertThat(result.getMeta().getVersion()).startsWith("W/\"");
    }

    @Test
    void update_negativeResult_throwsMappedStatus() {
        ScimUser resource = user("K_UPD_404");
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.ENDRE), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, -1, "missing", null, "404"));

        ResourceException ex = assertThrows(ResourceException.class,
                () -> provider.update("K_UPD_404", null, resource, null, null));

        assertThat(ex.getStatus()).isEqualTo(404);
    }

    @Test
    void update_success_returnsUserFromView_andLogs200() throws ResourceException {
        ReflectionTestUtils.setField(provider, "syncEnabled", true);

        ScimUser resource = user("K_UPD_OK");
        ScimUser fromView = user("K_UPD_OK");
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.ENDRE), anyString()))
                .thenReturn(new PlsqlProcedureResult("payload", 0, "ok", null, "0"));
        when(userService.getUser("K_UPD_OK")).thenReturn(Optional.of(fromView));

        ScimUser result = provider.update("K_UPD_OK", null, resource, null, null);

        assertThat(result).isSameAs(fromView);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_PUT), eq("/scim/v2/Users/K_UPD_OK"), eq(200), anyLong(), anyString(), eq(null), eq(null));
    }

    @Test
    void delete_negativeResult_throwsMappedStatusAndRethrowsResourceException() {
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.SLETTE), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, -1, "missing", null, "404"));

        ResourceException ex = assertThrows(ResourceException.class, () -> provider.delete("K_DEL_404"));

        assertThat(ex.getStatus()).isEqualTo(404);
    }

    @Test
    void toJson_returnsNull_whenObjectCannotBeSerialized() {
        Map<String, Object> cyclic = new HashMap<>();
        cyclic.put("self", cyclic);

        String json = ReflectionTestUtils.invokeMethod(provider, "toJson", cyclic);

        assertThat(json).isNull();
    }

    @Test
    void plsqlResponse_escapesQuotesAndHandlesNullData() {
        String response = ReflectionTestUtils.invokeMethod(provider, "plsqlResponse",
                new PlsqlProcedureResult("hello\"data", 0, "err\"msg", 55L, "0"));

        assertThat(response)
                .contains("\"interfaceMsgId\":55")
                .contains("\"errbuf\":\"err'msg\"")
                .contains("\"data\":\"hello'data\"");
    }

    @Test
    void create_logsProcedureResponseWithData_whenResultContainsData() throws ResourceException {
        ScimUser resource = user("K_DATA");
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.NY), anyString()))
                .thenReturn(new PlsqlProcedureResult("abc\"def", 0, "ok", null, "0"));
        when(userService.getUser("K_DATA")).thenReturn(Optional.of(resource));

        provider.create(resource);

        verify(kallLoggHelper).loggUt(eq(KallLogg.METHOD_POST), any(), eq(201), anyLong(), anyString(), contains("\"data\":\"abc'def\""), eq("ok"));
    }

    @ParameterizedTest
    @MethodSource("responseWithNullRetcodeCases")
    void response_helpers_handleNullRetcode(String methodName,
                                            PlsqlProcedureResult result,
                                            List<String> expectedSnippets) {
        String response = ReflectionTestUtils.invokeMethod(provider, methodName, result);

        assertThat(response).contains(expectedSnippets.toArray(new String[0]));
    }

    private static Stream<Arguments> responseWithNullRetcodeCases() {
        return Stream.of(
                Arguments.of(
                        "plsqlResponse",
                        new PlsqlProcedureResult(null, 0, "ok", null, null),
                        List.of("\"retcode\":\"\"")
                ),
                Arguments.of(
                        "plsqlResponse",
                        new PlsqlProcedureResult("data", 0, null, null, null),
                        List.of("\"retcode\":\"\"", "\"errbuf\":null")
                ),
                Arguments.of(
                        "plsqlSyncResponse",
                        new PlsqlProcedureResult(null, 0, null, null, null, null, null),
                        List.of("\"retcode\":\"\"", "\"devPhase\":\"\"", "\"devStatus\":\"\"", "\"errbuf\":null")
                )
        );
    }

    @Test
    void errorJson_usesUnknownDetail_whenDetailIsNull() {
        String json = ReflectionTestUtils.invokeMethod(provider, "errorJson", 500, null);

        assertThat(json).contains("\"detail\":\"Ukjent feil\"");
    }

    @Test
    void errorJson_returnsHardcodedFallback_whenSerializationThrows() throws Exception {
        ScimUserResourceProvider failingProvider = spy(provider);
        doThrow(new JsonProcessingException("boom") { }).when(failingProvider).serializeToJson(any());

        String json = ReflectionTestUtils.invokeMethod(failingProvider, "errorJson", 500, "detalj");

        assertThat(json).isEqualTo("{\"schemas\":[\"urn:ietf:params:scim:api:messages:2.0:Error\"],\"status\":\"500\",\"detail\":\"Ukjent feil\"}");
    }

    private static ScimUser user(String id) {
        ScimUser user = new ScimUser();
        user.setId(id);
        user.setUserName("ABC1234");
        return user;
    }
}


