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
     * Hent en bruker basert på userName (brukerId)
     */
    public Optional<ScimUser> getUser(String userName) {
        log.debug("Henter bruker: {}", userName);

        Optional<ScimUserEntity> userEntity = userRepository.findByBrukerId(userName);

        if (userEntity.isEmpty()) {
            log.debug("Bruker ikke funnet: {}", userName);
            return Optional.empty();
        }

        // Hent gruppe-medlemskap
        List<ScimGroupMembershipEntity> groups = groupMembershipRepository.findByBrukerId(userName);

        ScimUser scimUser = userMapper.toScimUser(userEntity.get(), groups);

        log.debug("Bruker funnet: {} med {} grupper", userName, groups.size());
        return Optional.of(scimUser);
    }

    /**
     * Hent bruker basert på externalId (navId)
     */
    public Optional<ScimUser> getUserByExternalId(Long navId) {
        log.debug("Henter bruker med navId: {}", navId);

        Optional<ScimUserEntity> userEntity = userRepository.findByNavId(navId);

        if (userEntity.isEmpty()) {
            log.debug("Bruker med navId {} ikke funnet", navId);
            return Optional.empty();
        }

        List<ScimGroupMembershipEntity> groups =
            groupMembershipRepository.findByBrukerId(userEntity.get().getBrukerId());

        ScimUser scimUser = userMapper.toScimUser(userEntity.get(), groups);
        return Optional.of(scimUser);
    }

    /**
     * Hent alle aktive brukere (paginert)
     */
    public Page<ScimUser> getUsers(int startIndex, int count) {
        log.debug("Henter brukere: startIndex={}, count={}", startIndex, count);

        // SCIM bruker 1-basert indeksering, Spring Data bruker 0-basert
        int pageNumber = (startIndex - 1) / count;
        Pageable pageable = PageRequest.of(pageNumber, count);

        Page<ScimUserEntity> userPage = userRepository.findAllActiveUsers(pageable);

        // Konverter til SCIM Users (uten grupper for ytelse)
        Page<ScimUser> scimUserPage = userPage.map(entity ->
            userMapper.toScimUser(entity, List.of())
        );

        log.debug("Hentet {} av {} brukere", scimUserPage.getNumberOfElements(),
                  scimUserPage.getTotalElements());

        return scimUserPage;
    }

    /**
     * Tell totalt antall aktive brukere
     */
    public long getTotalUsers() {
        return userRepository.countActiveUsers();
    }

    /**
     * Hent alle grupper for en bruker
     */
    public List<ScimGroupMembershipEntity> getUserGroups(String userName) {
        return groupMembershipRepository.findByBrukerId(userName);
    }
}
