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
        long t0 = System.currentTimeMillis();
        log.info("[GroupsWithMembers] starter");

        List<ScimGroupMembershipEntity> allMemberships = membershipRepository.findAll();
        long t1 = System.currentTimeMillis();
        log.info("[GroupsWithMembers] membershipRepository.findAll() – {} rader – {}ms",
            allMemberships.size(), t1 - t0);

        Map<String, List<ScimGroupMembershipEntity>> membersByGroup = allMemberships.stream()
            .filter(m -> m.getScimGroupId() != null && m.getNavId() != null)
            .collect(Collectors.groupingBy(ScimGroupMembershipEntity::getScimGroupId));
        long t2 = System.currentTimeMillis();
        log.info("[GroupsWithMembers] gruppering – {} grupper – {}ms", membersByGroup.size(), t2 - t1);

        Map<String, ScimGroupEntity> groupById = groupRepository
            .findAllById(List.copyOf(membersByGroup.keySet()))
            .stream()
            .collect(Collectors.toMap(ScimGroupEntity::getScimId, g -> g));
        long t3 = System.currentTimeMillis();
        log.info("[GroupsWithMembers] groupRepository.findAllById() – {} entiteter – {}ms",
            groupById.size(), t3 - t2);

        List<ScimGroup> result = membersByGroup.entrySet().stream()
            .filter(e -> groupById.containsKey(e.getKey()))
            .map(e -> groupMapper.toScimGroup(groupById.get(e.getKey()), e.getValue()))
            .sorted(Comparator.comparing(ScimGroup::getId))
            .toList();
        long t4 = System.currentTimeMillis();
        log.info("[GroupsWithMembers] mapping – {} ScimGroup-objekter – {}ms", result.size(), t4 - t3);
        log.info("[GroupsWithMembers] ferdig – totalt {}ms", t4 - t0);

        return result;
    }

    /**
     * Alle aktive brukere med kun id (navId) og groups-array.
     * Ingen andre brukerattributter er inkludert.
     */
    public List<ScimUser> getUserMemberships() {
        long t0 = System.currentTimeMillis();
        log.info("[UserMemberships] starter");

        List<ScimGroupMembershipEntity> allMemberships = membershipRepository.findAll();
        long t1 = System.currentTimeMillis();
        log.info("[UserMemberships] membershipRepository.findAll() – {} rader – {}ms",
            allMemberships.size(), t1 - t0);

        Map<String, List<ScimGroupMembershipEntity>> groupsByNavId = allMemberships.stream()
            .filter(m -> m.getNavId() != null && m.getScimGroupId() != null)
            .collect(Collectors.groupingBy(ScimGroupMembershipEntity::getNavId));
        long t2 = System.currentTimeMillis();
        log.info("[UserMemberships] gruppering – {} brukere – {}ms", groupsByNavId.size(), t2 - t1);

        List<ScimUser> result = groupsByNavId.entrySet().stream()
            .map(e -> userMapper.toSlimScimUser(e.getKey(), e.getValue()))
            .sorted(Comparator.comparing(ScimUser::getId))
            .toList();
        long t3 = System.currentTimeMillis();
        log.info("[UserMemberships] mapping – {} ScimUser-objekter – {}ms", result.size(), t3 - t2);
        log.info("[UserMemberships] ferdig – totalt {}ms", t3 - t0);

        return result;
    }
}
