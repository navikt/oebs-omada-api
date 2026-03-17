package no.nav.oebs.api.scim.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.scim.ScimGroupEntity;
import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import no.nav.oebs.api.scim.mapper.ScimGroupMapper;
import no.nav.oebs.api.scim.mapper.ScimUserMapper;
import no.nav.oebs.api.scim.repository.ScimGroupMembershipRepository;
import no.nav.oebs.api.scim.repository.ScimGroupRepository;
import org.apache.directory.scim.spec.resources.ScimGroup;
import org.apache.directory.scim.spec.resources.ScimUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Henter data for de to Omada bulk-endepunktene.
 * Returnerer SCIMple-typer (ScimGroup / ScimUser) som Jersey serialiserer korrekt.
 *
 * @see no.nav.oebs.api.scim.resource.GroupsWithMembersResource
 * @see no.nav.oebs.api.scim.resource.UserMembershipsResource
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OmadaApiService {

    private final ScimGroupRepository groupRepository;
    private final ScimGroupMembershipRepository membershipRepository;
    private final ScimGroupMapper groupMapper;
    private final ScimUserMapper userMapper;

    /**
     * Alle grupper/ansvarsområder med minst ett aktivt medlem.
     * Members-arrayet er populert med navId-er.
     */
    public List<ScimGroup> getGroupsWithMembers() {
        log.debug("Henter alle grupper med aktive medlemmer");

        List<ScimGroupMembershipEntity> allMemberships = membershipRepository.findAll();

        Map<String, List<ScimGroupMembershipEntity>> membersByGroup = allMemberships.stream()
            .collect(Collectors.groupingBy(ScimGroupMembershipEntity::getScimGroupId));

        Map<String, ScimGroupEntity> groupById = groupRepository
            .findAllById(List.copyOf(membersByGroup.keySet()))
            .stream()
            .collect(Collectors.toMap(ScimGroupEntity::getScimId, g -> g));

        List<ScimGroup> result = membersByGroup.entrySet().stream()
            .filter(e -> groupById.containsKey(e.getKey()))
            .map(e -> groupMapper.toScimGroup(groupById.get(e.getKey()), e.getValue()))
            .sorted(Comparator.comparing(ScimGroup::getId))
            .toList();

        log.debug("Returnerer {} grupper med totalt {} medlemskap", result.size(), allMemberships.size());
        return result;
    }

    /**
     * Alle aktive brukere med kun id (navId) og groups-array.
     * Ingen andre brukerattributter er inkludert.
     */
    public List<ScimUser> getUserMemberships() {
        log.debug("Henter brukere med gruppemedlemskap");

        List<ScimGroupMembershipEntity> allMemberships = membershipRepository.findAll();

        Map<String, List<ScimGroupMembershipEntity>> groupsByNavId = allMemberships.stream()
            .collect(Collectors.groupingBy(ScimGroupMembershipEntity::getNavId));

        List<ScimUser> result = groupsByNavId.entrySet().stream()
            .map(e -> userMapper.toSlimScimUser(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(ScimUser::getId))
            .toList();

        log.debug("Returnerer {} brukere med gruppemedlemskap", result.size());
        return result;
    }
}

