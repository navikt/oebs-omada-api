package no.nav.oebs.api.scim.provider;

import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.scim.KallLoggHelper;
import no.nav.oebs.api.scim.service.ScimGroupService;
import org.apache.directory.scim.spec.exception.ResourceException;
import org.apache.directory.scim.spec.filter.FilterResponse;
import org.apache.directory.scim.spec.filter.PageRequest;
import org.apache.directory.scim.spec.resources.ScimGroup;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScimGroupResourceProviderTest {

    @Mock
    private ScimGroupService groupService;

    @Mock
    private KallLoggHelper kallLoggHelper;

    @InjectMocks
    private ScimGroupResourceProvider provider;

    @Test
    void get_returnsGroup_whenFound() throws Exception {
        ScimGroup group = new ScimGroup();
        group.setId("G$1234");
        when(groupService.getGroup("G$1234")).thenReturn(Optional.of(group));

        ScimGroup result = provider.get("G$1234");

        assertThat(result).isSameAs(group);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_GET), eq("/scim/v2/Groups/G$1234"), eq(200), any(Long.class), any(String.class), eq((String) null));
    }

    @Test
    void get_returnsNull_whenNotFound() throws Exception {
        when(groupService.getGroup("G$UKJENT")).thenReturn(Optional.empty());

        ScimGroup result = provider.get("G$UKJENT");

        assertThat(result).isNull();
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_GET), eq("/scim/v2/Groups/G$UKJENT"), eq(404), any(Long.class), eq((String) null), eq("Group not found"));
    }

    @Test
    void get_throwsResourceException500_whenServiceFails() {
        when(groupService.getGroup("G$ERR")).thenThrow(new RuntimeException("boom"));

        ResourceException ex = assertThrows(ResourceException.class, () -> provider.get("G$ERR"));

        assertThat(ex.getStatus()).isEqualTo(500);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_GET), eq("/scim/v2/Groups/G$ERR"), eq(500), any(Long.class), contains("\"status\":\"500\""), eq((String) null));
    }

    @Test
    void find_returnsFilterResponse_withDefaultsWhenPageRequestNull() {
        ScimGroup group = new ScimGroup();
        group.setId("G$1234");
        Page<ScimGroup> page = new PageImpl<>(List.of(group));
        when(groupService.getGroups(1, 100)).thenReturn(page);

        FilterResponse<ScimGroup> response = provider.find(null, null, null);

        assertThat(response.getResources()).containsExactly(group);
        assertThat(response.getTotalResults()).isEqualTo(1);
        verify(groupService).getGroups(1, 100);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_GET), eq("/scim/v2/Groups"), eq(200), any(Long.class), eq((String) null), contains("totalResults=1"));
    }

    @Test
    void find_usesStartIndexAndCount_fromPageRequest() {
        PageRequest pageRequest = new PageRequest().setStartIndex(11).setCount(10);
        when(groupService.getGroups(11, 10)).thenReturn(Page.empty());

        provider.find(null, pageRequest, null);

        verify(groupService).getGroups(11, 10);
    }

    @Test
    void find_throwsRuntimeException_whenServiceFails() {
        when(groupService.getGroups(1, 100)).thenThrow(new RuntimeException("down"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> provider.find(null, null, null));

        assertThat(ex.getMessage()).contains("Intern feil");
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_GET), eq("/scim/v2/Groups"), eq(500), any(Long.class), contains("\"status\":\"500\""), eq((String) null));
    }

    @Test
    void create_throws405() {
        ResourceException ex = assertThrows(ResourceException.class, () -> provider.create(new ScimGroup()));
        assertThat(ex.getStatus()).isEqualTo(405);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_POST), eq("/scim/v2/Groups"), eq(405), eq(0L), contains("ikke st\u00f8ttet"), eq((String) null));
    }

    @Test
    void update_throws405() {
        ResourceException ex = assertThrows(ResourceException.class,
                () -> provider.update("G$1234", null, new ScimGroup(), null, null));
        assertThat(ex.getStatus()).isEqualTo(405);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_PUT), eq("/scim/v2/Groups/G$1234"), eq(405), eq(0L), contains("ikke st\u00f8ttet"), eq((String) null));
    }

    @Test
    void patch_throws405() {
        ResourceException ex = assertThrows(ResourceException.class,
                () -> provider.patch("G$1234", null, List.of(), null, null));
        assertThat(ex.getStatus()).isEqualTo(405);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_PUT), eq("/scim/v2/Groups/G$1234"), eq(405), eq(0L), contains("ikke st\u00f8ttet"), eq((String) null));
    }

    @Test
    void delete_throws405() {
        ResourceException ex = assertThrows(ResourceException.class, () -> provider.delete("G$1234"));
        assertThat(ex.getStatus()).isEqualTo(405);
        verify(kallLoggHelper).loggInn(eq(KallLogg.METHOD_DELETE), eq("/scim/v2/Groups/G$1234"), eq(405), eq(0L), contains("ikke st\u00f8ttet"), eq((String) null));
    }
}

