package no.nav.oebs.api.scim.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import no.nav.oebs.api.scim.ScimUserEntity;
import no.nav.oebs.api.scim.mapper.ScimUserMapper;
import no.nav.oebs.api.scim.repository.ScimGroupMembershipRepository;
import no.nav.oebs.api.scim.repository.ScimUserRepository;
import org.apache.directory.scim.spec.resources.ScimUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for SCIM User operasjoner
 * Henter data fra database views og konverterer til SCIM format
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScimUserService {

    private final ScimUserRepository userRepository;
    private final ScimGroupMembershipRepository groupMembershipRepository;
    private final ScimUserMapper userMapper;

    /**
     * Hent en bruker basert på SCIM id = navId (f.eks. "S108633")
     */
    public Optional<ScimUser> getUser(String id) {
        log.debug("Henter bruker med navId: {}", id);

        Optional<ScimUserEntity> userEntity = userRepository.findByNavId(id);

        if (userEntity.isEmpty()) {
            log.debug("Bruker ikke funnet for navId: {}", id);
            return Optional.empty();
        }

        ScimUserEntity entity = userEntity.get();
        List<ScimGroupMembershipEntity> groups = groupMembershipRepository.findByNavId(entity.getNavId());
        ScimUser scimUser = userMapper.toScimUser(entity, groups);

        log.debug("Bruker funnet: navId={}, brukerId={}, grupper={}, egenansatt={}",
                id, entity.getBrukerId(), groups.size(), entity.isEgenansatt());
        return Optional.of(scimUser);
    }

    /**
     * Hent alle aktive brukere (paginert)
     */
    @SuppressWarnings("NullableProblems")
    public Page<ScimUser> getUsers(int startIndex, int count) {
        log.debug("Henter brukere: startIndex={}, count={}", startIndex, count);

        int pageNumber = (startIndex - 1) / count;
        Pageable pageable = PageRequest.of(pageNumber, count);

        Page<ScimUserEntity> userPage = userRepository.findAllActiveUsers(pageable);

        Page<ScimUser> scimUserPage = userPage.map(entity ->
            userMapper.toScimUser(entity, List.of())
        );

        log.debug("Hentet {} av {} brukere", scimUserPage.getNumberOfElements(),
                  scimUserPage.getTotalElements());

        return scimUserPage;
    }
}
