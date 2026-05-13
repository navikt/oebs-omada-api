package no.nav.oebs.api.scim.service;

import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import no.nav.oebs.api.scim.ScimUserEntity;
import no.nav.oebs.api.scim.mapper.ScimUserMapper;
import no.nav.oebs.api.scim.repository.ScimGroupMembershipRepository;
import no.nav.oebs.api.scim.repository.ScimUserRepository;
import org.apache.directory.scim.spec.resources.ScimUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScimUserServiceTest {

    @Mock ScimUserRepository       userRepository;
    @Mock ScimGroupMembershipRepository groupMembershipRepository;
    @Mock ScimUserMapper           userMapper;

    @InjectMocks
    ScimUserService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "userMapper", userMapper);
    }

    // -------------------------------------------------------------------------
    // getUser()
    // -------------------------------------------------------------------------

    @Test
    void getUser_returnsEmpty_whenNotFound() {
        when(userRepository.findByNavId("UKJENT")).thenReturn(Optional.empty());

        assertThat(service.getUser("UKJENT")).isEmpty();
        verifyNoInteractions(groupMembershipRepository, userMapper);
    }

    @Test
    void getUser_returnsUser_whenFound() {
        ScimUserEntity entity = enBruker();
        when(userRepository.findByNavId("S123456")).thenReturn(Optional.of(entity));
        when(groupMembershipRepository.findByNavId("S123456")).thenReturn(List.of());
        when(userMapper.toScimUser(any(), any())).thenReturn(new ScimUser());

        Optional<ScimUser> result = service.getUser("S123456");

        assertThat(result).isPresent();
        verify(groupMembershipRepository).findByNavId("S123456");
        verify(userMapper).toScimUser(eq(entity), any());
    }

    @Test
    void getUser_passesGroupsToMapper() {
        ScimUserEntity entity = enBruker();
        ScimGroupMembershipEntity gruppe = new ScimGroupMembershipEntity();
        gruppe.setScimGroupId("GRP001");

        when(userRepository.findByNavId("S123456")).thenReturn(Optional.of(entity));
        when(groupMembershipRepository.findByNavId("S123456")).thenReturn(List.of(gruppe));
        when(userMapper.toScimUser(any(), any())).thenReturn(new ScimUser());

        service.getUser("S123456");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ScimGroupMembershipEntity>> groupCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(userMapper).toScimUser(any(), groupCaptor.capture());
        assertThat(groupCaptor.getValue()).containsExactly(gruppe);
    }

    // -------------------------------------------------------------------------
    // getUsers()
    // -------------------------------------------------------------------------

    @Test
    void getUsers_returnsMappedPage() {
        ScimUserEntity entity = enBruker();
        Page<ScimUserEntity> page = new PageImpl<>(List.of(entity));

        when(userRepository.findAllActiveUsers(any(Pageable.class))).thenReturn(page);
        when(userMapper.toScimUser(any(), eq(List.of()))).thenReturn(new ScimUser());

        @SuppressWarnings("NullableProblems")
        Page<ScimUser> result = service.getUsers(1, 10);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userMapper).toScimUser(entity, List.of());
    }

    @Test
    void getUsers_calculatesCorrectPageNumber() {
        when(userRepository.findAllActiveUsers(any(Pageable.class)))
                .thenReturn(Page.empty());

        service.getUsers(21, 10); // startIndex=21, count=10 → side 2 (0-basert)

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findAllActiveUsers(pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(10);
    }

    private ScimUserEntity enBruker() {
        ScimUserEntity e = new ScimUserEntity();
        e.setNavId("S123456");
        e.setBrukerId("ABC1234");
        e.setForNavn("Ola");
        e.setEtterNavn("Nordmann");
        e.setActiveFlag("Y");
        return e;
    }
}

