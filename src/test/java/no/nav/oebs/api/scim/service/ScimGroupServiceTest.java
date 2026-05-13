package no.nav.oebs.api.scim.service;

import no.nav.oebs.api.scim.ScimGroupEntity;
import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import no.nav.oebs.api.scim.mapper.ScimGroupMapper;
import no.nav.oebs.api.scim.repository.ScimGroupMembershipRepository;
import no.nav.oebs.api.scim.repository.ScimGroupRepository;
import org.apache.directory.scim.spec.resources.ScimGroup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScimGroupServiceTest {

    @Mock
    private ScimGroupRepository groupRepository;

    @Mock
    private ScimGroupMembershipRepository groupMembershipRepository;

    @Mock
    private ScimGroupMapper groupMapper;

    @InjectMocks
    private ScimGroupService service;

    @Test
    void getGroup_returnsEmpty_whenNotFound() {
        when(groupRepository.findByScimId("G$UKJENT")).thenReturn(Optional.empty());

        assertThat(service.getGroup("G$UKJENT")).isEmpty();
        verifyNoInteractions(groupMembershipRepository, groupMapper);
    }

    @Test
    void getGroup_returnsMappedGroup_whenFound() {
        ScimGroupEntity entity = enGruppe();
        ScimGroupMembershipEntity medlem = enMedlem();
        ScimGroup mapped = new ScimGroup();

        when(groupRepository.findByScimId("G$1234")).thenReturn(Optional.of(entity));
        when(groupMembershipRepository.findByScimGroupId("G$1234")).thenReturn(List.of(medlem));
        when(groupMapper.toScimGroup(entity, List.of(medlem))).thenReturn(mapped);

        Optional<ScimGroup> result = service.getGroup("G$1234");

        assertThat(result).containsSame(mapped);
        verify(groupMembershipRepository).findByScimGroupId("G$1234");
        verify(groupMapper).toScimGroup(entity, List.of(medlem));
    }

    @Test
    void getGroups_mapsPageAndUsesExpectedPaging() {
        ScimGroupEntity entity = enGruppe();
        ScimGroup mapped = new ScimGroup();
        Page<ScimGroupEntity> page = new PageImpl<>(List.of(entity));

        when(groupRepository.findAllByOrderByScimIdAsc(any(Pageable.class))).thenReturn(page);
        when(groupMapper.toScimGroup(entity, null)).thenReturn(mapped);

        Page<ScimGroup> result = service.getGroups(1, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).containsExactly(mapped);
        verify(groupMapper).toScimGroup(entity, null);
    }

    @Test
    void getGroups_calculatesCorrectPageNumber() {
        when(groupRepository.findAllByOrderByScimIdAsc(any(Pageable.class))).thenReturn(Page.empty());

        service.getGroups(21, 10); // 1-based startIndex -> page 2 (0-based)

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(groupRepository).findAllByOrderByScimIdAsc(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
    }

    @Test
    void getTotalGroups_delegatesToRepository() {
        when(groupRepository.countAllGroupsAndResponsibilities()).thenReturn(42L);

        assertThat(service.getTotalGroups()).isEqualTo(42L);
        verify(groupRepository).countAllGroupsAndResponsibilities();
    }

    @Test
    void getGroupMembers_delegatesToRepository() {
        ScimGroupMembershipEntity medlem = enMedlem();
        when(groupMembershipRepository.findByScimGroupId("G$1234")).thenReturn(List.of(medlem));

        List<ScimGroupMembershipEntity> result = service.getGroupMembers("G$1234");

        assertThat(result).containsExactly(medlem);
        verify(groupMembershipRepository).findByScimGroupId("G$1234");
    }

    private ScimGroupEntity enGruppe() {
        ScimGroupEntity entity = new ScimGroupEntity();
        entity.setScimId("G$1234");
        entity.setScimDisplayName("NAV IT");
        entity.setKildeType("G");
        entity.setKildeId(1234L);
        return entity;
    }

    private ScimGroupMembershipEntity enMedlem() {
        ScimGroupMembershipEntity medlem = new ScimGroupMembershipEntity();
        medlem.setScimGroupId("G$1234");
        medlem.setNavId("S123456");
        return medlem;
    }
}

