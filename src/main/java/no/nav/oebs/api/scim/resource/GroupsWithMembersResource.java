package no.nav.oebs.api.scim.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.scim.service.OmadaApiService;
import org.apache.directory.scim.spec.resources.ScimGroup;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JAX-RS ressurs registrert i SCIMple Jersey-servleten.
 * Tilgjengelig på GET /scim/v2/GroupsWithMembers
 *
 * Returnerer alle grupper og ansvarsområder som har minst ett aktivt medlem.
 * Hvert Group-objekt er komplett med members-array populert med navId-er.
 * Respons er en SCIM 2.0 ListResponse.
 */
@Slf4j
@Path("GroupsWithMembers")
public class GroupsWithMembersResource {

    private static final String LIST_RESPONSE_SCHEMA =
        "urn:ietf:params:scim:api:messages:2.0:ListResponse";

    private final OmadaApiService omadaApiService;

    @Inject
    @SuppressWarnings("CdiInjectionPointsInspection")
    public GroupsWithMembersResource(OmadaApiService omadaApiService) {
        this.omadaApiService = omadaApiService;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroupsWithMembers() {
        long start = System.currentTimeMillis();

        List<ScimGroup> groups = omadaApiService.getGroupsWithMembers();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schemas",      List.of(LIST_RESPONSE_SCHEMA));
        response.put("totalResults", groups.size());
        response.put("startIndex",   1);
        response.put("itemsPerPage", groups.size());
        response.put("Resources",    groups);

        long total = System.currentTimeMillis() - start;
        if (total > 10_000) {
            log.warn("[GroupsWithMembers] TREG RESPONS – {} grupper – total {}ms (inkl. serialisering)", groups.size(), total);
        } else {
            log.info("[GroupsWithMembers] total inkl. serialisering {}ms", total);
        }

        return Response.ok(response).build();
    }
}

