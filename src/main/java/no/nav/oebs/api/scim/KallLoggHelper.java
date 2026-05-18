package no.nav.oebs.api.scim;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.config.common.mdc.MdcOperations;
import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.KallLoggRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Felles hjelper for logging av SCIM-kall til KallLogg-tabellen.
 *
 * Konvensjon:
 *   INN  (TYPE_REST)  — inngående kall fra Omada: request=Omada-payload, response=svar til Omada
 *   UT   (TYPE_PLSQL) — utgående kall til OEBS/PL/SQL: request=JSON vi sender, response=OEBS-resultat
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KallLoggHelper {

    private final KallLoggRepository kallLoggRepository;

    /**
     * Logger et inngående kall fra Omada med request-body (POST/PUT/DELETE).
     * request  = payload Omada sendte oss
     * response = svaret vi returnerer til Omada
     */
    public void loggInn(String method, String operation, int status,
                        long kalltid, String request, String response, String logginfo) {
        save(KallLogg.RETNING_INN, KallLogg.TYPE_REST, method, operation, status, kalltid, request, response, logginfo);
    }

    /**
     * Logger et inngående kall fra Omada uten request-body (GET).
     * response = svaret vi returnerer til Omada
     */
    public void loggInn(String method, String operation, int status,
                        long kalltid, String response, String logginfo) {
        save(KallLogg.RETNING_INN, KallLogg.TYPE_REST, method, operation, status, kalltid, null, response, logginfo);
    }

    /**
     * Logger et utgående kall til OEBS (PL/SQL-prosedyre).
     * operation = prosedyrenavn (f.eks. APPS.XXRTV_INT_OMADA_INSERT_MESSAGE.InsertOmadaMessage)
     * request   = JSON vi sendte til OEBS
     * response  = data OEBS returnerte (errbuf / data)
     */
    public void loggUt(String method, String operation, int status,
                     long kalltid, String request, String response, String logginfo) {
        save(KallLogg.RETNING_UT, KallLogg.TYPE_PLSQL, method, operation, status, kalltid, request, response, logginfo);
    }

    private void save(String retning, String type, String method, String operation, int status,
                      long kalltid, String request, String response, String logginfo) {
        String korrelasjonId = MdcOperations.get(MdcOperations.MDC_CORRELATION_ID);
        String safeMethod = sanitizeForLog(method);
        String safeOperation = sanitizeForLog(operation);
        String safeKorrelasjonId = sanitizeForLog(korrelasjonId);

        log.info("[{}][{}] {} {} – status={} kalltid={}ms korrelasjonId={} requestPresent={} responsePresent={} logginfoPresent={} requestLength={} responseLength={} logginfoLength={}",
          retning, type, safeMethod, safeOperation, status, kalltid, safeKorrelasjonId,
          request != null, response != null, logginfo != null,
          request == null ? 0 : request.length(),
          response == null ? 0 : response.length(),
          logginfo == null ? 0 : logginfo.length());
        

        KallLogg kallLogg = KallLogg.builder()
                .korrelasjonId(korrelasjonId)
                .tidspunkt(LocalDateTime.now())
                .type(type)
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

    private String sanitizeForLog(String value) {
        if (value == null) return null;
        return value
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
                .replaceAll("\\p{Cntrl}", "_");
    }
}
