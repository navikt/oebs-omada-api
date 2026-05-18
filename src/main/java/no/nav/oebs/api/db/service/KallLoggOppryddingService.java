package no.nav.oebs.api.db.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.KallLoggRepository;
import no.nav.oebs.api.scim.KallLoggHelper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Rydder opp i XXRTV_OMADA_LOG-tabellen ved å slette rader eldre enn
 * {@code oebs.kalllogg.retention-days} dager (standard 30).
 *
 * Kjører daglig kl. 02:00 som standard — kan overstyres via
 * {@code oebs.kalllogg.cleanup-cron}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KallLoggOppryddingService {

    private static final String KALLLOGG_OPPRYDDING_KILDE = "XXRTV_OMADA_LOG opprydding";

    private final KallLoggRepository kallLoggRepository;
    private final KallLoggHelper kallLoggHelper;

    @Value("${oebs.kalllogg.retention-days:30}")
    private int retentionDays;

    /**
     * Sletter alle KallLogg-rader eldre enn {@code retentionDays} dager.
     * Kjøres daglig — cron kan overstyres med {@code oebs.kalllogg.cleanup-cron}.
     */
    @Scheduled(cron = "${oebs.kalllogg.cleanup-cron:0 0 2 * * *}")
    @Transactional
    public void ryddOppGamleRader() {
        LocalDateTime grense = LocalDateTime.now().minusDays(retentionDays);

        long totaltFor   = kallLoggRepository.count();
        long skalSlettes = kallLoggRepository.tellGamleRader(grense);
        log.info("KallLogg opprydding starter — totalt {} rader, {} eldre enn {} dager vil bli slettet (tidspunkt < {})",
                totaltFor, skalSlettes, retentionDays, grense);

        if (skalSlettes == 0) {
            log.info("KallLogg opprydding — ingenting å slette");
            kallLoggHelper.loggUt(KallLogg.METHOD_DELETE, KALLLOGG_OPPRYDDING_KILDE, 200, 0,
                    requestJson(grense, retentionDays, totaltFor, skalSlettes),
                    "{\"slettet\":0,\"totaltEtter\":" + totaltFor + "}",
                    null);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            int slettet      = kallLoggRepository.slettGamleRader(grense);
            long totaltEtter = kallLoggRepository.count();
            long kalltid     = System.currentTimeMillis() - start;
            log.info("KallLogg opprydding fullført — slettet {} av {} rader på {}ms, {} rader gjenstår",
                    slettet, skalSlettes, kalltid, totaltEtter);
            kallLoggHelper.loggUt(KallLogg.METHOD_DELETE, KALLLOGG_OPPRYDDING_KILDE, 200, kalltid,
                    requestJson(grense, retentionDays, totaltFor, skalSlettes),
                    "{\"slettet\":" + slettet + ",\"totaltEtter\":" + totaltEtter + "}",
                    null);
        } catch (RuntimeException e) {
            long kalltid = System.currentTimeMillis() - start;
            log.error("KallLogg opprydding feilet etter {}ms (skulle slette {} av {} rader): {}",
                    kalltid, skalSlettes, totaltFor, e.getMessage(), e);
            kallLoggHelper.loggUt(KallLogg.METHOD_DELETE, KALLLOGG_OPPRYDDING_KILDE, 500, kalltid,
                    requestJson(grense, retentionDays, totaltFor, skalSlettes),
                    null,
                    e.getMessage());
        }
    }

    private static String requestJson(LocalDateTime grense, int retentionDays, long totaltFor, long skalSlettes) {
        return String.format("{\"retentionDays\":%d,\"grense\":\"%s\",\"totaltFor\":%d,\"skalSlettes\":%d}",
                retentionDays, grense, totaltFor, skalSlettes);
    }
}
