package no.nav.oebs.api.scim.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.config.CacheConfig;
import no.nav.oebs.api.scim.ScimGroupMembershipEntity;
import no.nav.oebs.api.scim.repository.ScimGroupMembershipRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Ansvarlig for å laste og cache alle gruppe-medlemskap fra databasen.
 * <p>
 * <b>Oppstart:</b> {@link #warmCacheOnStartup()} kjøres synkront så snart applikasjonen
 * er klar ({@link ApplicationReadyEvent}). Cachen er varm før første HTTP-request treffer.
 * <p>
 * <b>Periodisk refresh:</b> {@link #refreshMemberships()} kjøres i bakgrunnen på et
 * konfigurerbart intervall og oppdaterer cachen uten å blokkere innkommende kall.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MembershipCacheService {

    private final ScimGroupMembershipRepository membershipRepository;

    /**
     * Varmer cachen synkront ved oppstart. Kjøres én gang etter at hele
     * applikasjonskonteksten er oppe. Blokkerer ikke helsesjekker eller andre beans,
     * men betyr at første HTTP-request alltid treffer en varm cache.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmCacheOnStartup() {
        log.info("[MembershipCache] oppvarmning ved oppstart – starter DB-kall");
        refreshMemberships();
        log.info("[MembershipCache] oppvarmning fullført – cache er varm");
    }

    /**
     * Returnerer alle gruppe-medlemskap. Første kall treffer DB (cache-miss);
     * påfølgende kall svarer fra Caffeine-cachen.
     */
    @Cacheable(cacheNames = CacheConfig.MEMBERSHIP_CACHE, key = "'all'")
    @Transactional(readOnly = true)
    public List<ScimGroupMembershipEntity> getAllMemberships() {
        log.info("[MembershipCache] cache-miss – henter fra DB");
        long t0 = System.currentTimeMillis();
        List<ScimGroupMembershipEntity> result = membershipRepository.findAll();
        log.info("[MembershipCache] lastet {} rader fra DB på {}ms",
                result.size(), System.currentTimeMillis() - t0);
        return result;
    }

    /**
     * Kjøres periodisk i bakgrunnen. {@code @CachePut} oppdaterer alltid cachen
     * med ferskt resultat – uavhengig av om den er varm eller kald.
     * Standard intervall: 15 min (konfigurerbart via {@code oebs.cache.membership-refresh-ms}).
     * Initial delay er kort fordi {@link #warmCacheOnStartup()} håndterer første lasting.
     */
    @CachePut(cacheNames = CacheConfig.MEMBERSHIP_CACHE, key = "'all'")
    @Scheduled(fixedDelayString  = "${oebs.cache.membership-refresh-ms:900000}",
               initialDelayString = "${oebs.cache.membership-initial-delay-ms:10000}")
    @Transactional(readOnly = true)
    public List<ScimGroupMembershipEntity> refreshMemberships() {
        log.info("[MembershipCache] planlagt refresh – henter fra DB");
        long t0 = System.currentTimeMillis();
        List<ScimGroupMembershipEntity> result = membershipRepository.findAll();
        long elapsed = System.currentTimeMillis() - t0;
        if (elapsed > 10_000) {
            log.warn("[MembershipCache] refresh ferdig – {} rader – {}ms (TREGT)", result.size(), elapsed);
        } else {
            log.info("[MembershipCache] refresh ferdig – {} rader – {}ms", result.size(), elapsed);
        }
        return result;
    }
}

