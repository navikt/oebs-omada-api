package no.nav.oebs.api.scim.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.scim.ScimGroupEntity;
import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import no.nav.oebs.api.scim.mapper.ScimGroupMapper;
import no.nav.oebs.api.scim.repository.ScimGroupMembershipRepository;
import no.nav.oebs.api.scim.repository.ScimGroupRepository;
import org.apache.directory.scim.spec.resources.ScimGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for SCIM Group operasjoner
 * Håndterer både grupper (G$) og ansvarsområder (A$)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScimGroupService {

    private final ScimGroupRepository groupRepository;
    private final ScimGroupMembershipRepository groupMembershipRepository;
    private final ScimGroupMapper groupMapper;

    /**
     * Hent en gruppe eller ansvarsområde basert på ID
     */
    public Optional<ScimGroup> getGroup(String groupId) {
        log.debug("Henter gruppe: {}", groupId);

        Optional<ScimGroupEntity> groupEntity = groupRepository.findByScimId(groupId);

        if (groupEntity.isEmpty()) {
            log.debug("Gruppe ikke funnet: {}", groupId);
            return Optional.empty();
        }

        // Hent medlemmer
        List<ScimGroupMembershipEntity> members = groupMembershipRepository.findByScimGroupId(groupId);

        ScimGroup scimGroup = groupMapper.toScimGroup(groupEntity.get(), members);

        log.debug("Gruppe funnet: {} med {} medlemmer", groupId, members.size());
        return Optional.of(scimGroup);
    }

    /**
     * Hent alle grupper og ansvarsområder (paginert)
     */
    public Page<ScimGroup> getGroups(int startIndex, int count) {
        log.debug("Henter grupper: startIndex={}, count={}", startIndex, count);

        // SCIM bruker 1-basert indeksering, Spring Data bruker 0-basert
        int pageNumber = (startIndex - 1) / count;
        Pageable pageable = PageRequest.of(pageNumber, count);

        Page<ScimGroupEntity> groupPage = groupRepository.findAllGroupsAndResponsibilities(pageable);

        // Konverter til SCIM Groups (uten medlemmer for ytelse)
        Page<ScimGroup> scimGroupPage = groupPage.map(entity ->
            groupMapper.toScimGroup(entity, null)
        );

        log.debug("Hentet {} av {} grupper", scimGroupPage.getNumberOfElements(),
                  scimGroupPage.getTotalElements());

        return scimGroupPage;
    }

    /**
     * Tell totalt antall grupper og ansvarsområder
     */
    public long getTotalGroups() {
        return groupRepository.countAllGroupsAndResponsibilities();
    }

    /**
     * Hent medlemmer av en gruppe
     */
    public List<ScimGroupMembershipEntity> getGroupMembers(String groupId) {
        return groupMembershipRepository.findByScimGroupId(groupId);
    }
}
