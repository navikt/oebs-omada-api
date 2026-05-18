package no.nav.oebs.api.scim.provider;

import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.PlsqlProcedureRepository;
import no.nav.oebs.api.db.repository.PlsqlProcedureResult;
import no.nav.oebs.api.scim.KallLoggHelper;
import no.nav.oebs.api.scim.service.ScimUserService;
import org.apache.directory.scim.spec.exception.ResourceException;
import org.apache.directory.scim.spec.filter.FilterResponse;
import org.apache.directory.scim.spec.resources.ScimUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScimUserResourceProviderTest {

    @Mock
    private ScimUserService userService;

    @Mock
    private PlsqlProcedureRepository plsqlRepository;

    @Mock
    private KallLoggHelper kallLoggHelper;

    @InjectMocks
    private ScimUserResourceProvider provider;

    @Test
    void get_returnsUser_whenFound() throws ResourceException {
        ScimUser user = new ScimUser();
        user.setId("K123456");
        when(userService.getUser("K123456")).thenReturn(Optional.of(user));

        ScimUser result = provider.get("K123456");

        assertThat(result).isSameAs(user);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_GET), eq("/scim/v2/Users/K123456"), eq(200), anyLong(), anyString(), eq(null));
    }

    @Test
    void get_returnsNull_whenNotFound() throws ResourceException {
        when(userService.getUser("K404")).thenReturn(Optional.empty());

        ScimUser result = provider.get("K404");

        assertThat(result).isNull();
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_GET), eq("/scim/v2/Users/K404"), eq(404), anyLong(), eq(null), eq("User not found"));
    }

    @Test
    void get_throwsResourceException500_whenServiceFails() {
        when(userService.getUser("KERR")).thenThrow(new RuntimeException("boom"));

        ResourceException ex = assertThrows(ResourceException.class, () -> provider.get("KERR"));

        assertThat(ex.getStatus()).isEqualTo(500);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_GET), eq("/scim/v2/Users/KERR"), eq(500), anyLong(), contains("\"status\":\"500\""), eq(null));
    }

    @Test
    void find_returnsFilterResponse_withDefaultsWhenPageRequestNull() {
        ScimUser user = new ScimUser();
        user.setId("K1");
        Page<ScimUser> page = new PageImpl<>(List.of(user));
        when(userService.getUsers(1, 100)).thenReturn(page);

        FilterResponse<ScimUser> response = provider.find(null, null, null);

        assertThat(response.getResources()).containsExactly(user);
        assertThat(response.getTotalResults()).isEqualTo(1);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_GET), eq("/scim/v2/Users"), eq(200), anyLong(), eq(null), contains("totalResults=1"));
    }

    @Test
    void find_throwsIllegalStateException_whenServiceFails() {
        when(userService.getUsers(anyInt(), anyInt())).thenThrow(new RuntimeException("down"));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> provider.find(null, null, null));

        assertThat(ex.getMessage()).contains("Intern feil");
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_GET), eq("/scim/v2/Users"), eq(500), anyLong(), contains("\"status\":\"500\""), eq(null));
    }

    @Test
    void create_throws400_whenIdAndExternalIdMissing() {
        ScimUser resource = new ScimUser();
        resource.setUserName("ABC1234");

        ResourceException ex = assertThrows(ResourceException.class, () -> provider.create(resource));

        assertThat(ex.getStatus()).isEqualTo(400);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_POST), eq("/scim/v2/Users"), eq(400), anyLong(), anyString(), contains("id eller externalId"), eq(null));
    }

    @Test
    void create_returnsUserFromView_whenProcedureSucceeds() throws ResourceException {
        ScimUser resource = new ScimUser();
        resource.setId("K123456");
        resource.setUserName("ABC1234");

        ScimUser fromView = new ScimUser();
        fromView.setId("K123456");

        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.NY), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, 0, "ok", null, "0"));
        when(userService.getUser("K123456")).thenReturn(Optional.of(fromView));

        ScimUser result = provider.create(resource);

        assertThat(result).isSameAs(fromView);
    }

    @Test
    void update_throws400_whenResourceIsNull() {
        ResourceException ex = assertThrows(ResourceException.class,
                () -> provider.update("K123456", null, null, null, null));

        assertThat(ex.getStatus()).isEqualTo(400);
    }

    @Test
    void delete_doesNotThrow_whenProcedureSucceeds() {
        when(plsqlRepository.executeInOutProcedure(any(), eq(PlsqlProcedureRepository.Operasjon.SLETTE), anyString()))
                .thenReturn(new PlsqlProcedureResult(null, 0, "ok", null, "0"));

        assertDoesNotThrow(() -> provider.delete("K123456"));
    }
}


