package no.nav.oebs.api.scim.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.scim.service.OmadaApiService;
import org.apache.directory.scim.spec.resources.ScimUser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JAX-RS ressurs registrert i SCIMple Jersey-servleten.
 * Tilgjengelig på GET /scim/v2/UserMemberships
 *
 * Returnerer alle aktive brukere med kun id (navId) og groups-array.
 * Ingen andre brukerattributter er inkludert.
 * Respons er en SCIM 2.0 ListResponse.
 */
@Slf4j
@Path("UserMemberships")
public class UserMembershipsResource {

    private static final String LIST_RESPONSE_SCHEMA =
        "urn:ietf:params:scim:api:messages:2.0:ListResponse";

    private final OmadaApiService omadaApiService;

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    public UserMembershipsResource(OmadaApiService omadaApiService) {
        this.omadaApiService = omadaApiService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserMemberships() {
        long start = System.currentTimeMillis();

        List<ScimUser> users = omadaApiService.getUserMemberships();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schemas",      List.of(LIST_RESPONSE_SCHEMA));
        response.put("totalResults", users.size());
        response.put("startIndex",   1);
        response.put("itemsPerPage", users.size());
        response.put("Resources",    users);

        long total = System.currentTimeMillis() - start;
        if (total > 10_000) {
            log.warn("[UserMemberships] TREG RESPONS – {} brukere – total {}ms (inkl. serialisering)", users.size(), total);
        } else {
            log.info("[UserMemberships] total inkl. serialisering {}ms", total);
        }

        return Response.ok(response).build();
    }
}

