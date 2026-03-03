package no.nav.oebs.api.scim.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.config.common.mdc.MdcOperations;
import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.KallLoggRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Felles hjelper for logging av SCIM-kall til KallLogg-tabellen.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KallLoggHelper {

    private final KallLoggRepository kallLoggRepository;

    /**
     * Logger et utgående kall (POST, PUT, DELETE) mot PL/SQL.
     */
    public void loggUt(String method, String operation, int status,
                     long kalltid, String request, String response, String logginfo) {
        save(KallLogg.RETNING_UT, method, operation, status, kalltid, request, response, logginfo);
    }

    /**
     * Logger et inngående kall (GET) fra Omada.
     */
    public void loggInn(String method, String operation, int status,
                        long kalltid, String response, String logginfo) {
        save(KallLogg.RETNING_INN, method, operation, status, kalltid, null, response, logginfo);
    }

    private void save(String retning, String method, String operation, int status,
                      long kalltid, String request, String response, String logginfo) {
        KallLogg kallLogg = KallLogg.builder()
                .korrelasjonId(MdcOperations.get(MdcOperations.MDC_CORRELATION_ID))
                .tidspunkt(LocalDateTime.now())
                .type(KallLogg.TYPE_REST)
                .kallRetning(retning)
                .method(method)
                .operation(operation)
                .status(status)
                .kalltid(kalltid)
                .request(request)
                .response(response)
                .logginfo(logginfo)
                .build();

        try {
            kallLoggRepository.save(kallLogg);
        } catch (Exception e) {
            log.error("Feil ved logging av kalloggdata til databasen; feilmelding=" + e.getMessage(), e);
        }
    }
}
