package no.nav.oebs.api.scim.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.PlsqlProcedureRepository;
import no.nav.oebs.api.db.repository.PlsqlProcedureResult;
import no.nav.oebs.api.scim.service.ScimUserService;
import org.apache.directory.scim.spec.resources.ScimUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JAX-RS Resource for SCIM 2.0 Users endpoint
 * GET /scim/v2/Users
 * GET /scim/v2/Users/{id}
 */
@Slf4j
@Component
@Path("/Users")
@Produces("application/scim+json")
@Consumes("application/scim+json")
public class ScimUsersResource {

    private static final String PLSQL_PROCEDURE_NAME = "XXRTV_INT_OMADA_INSERT_MESSAGE.InsertOmadaMessage";

    @Autowired
    private PlsqlProcedureRepository plsqlRepository;

    @Autowired
    private KallLoggHelper kallLoggHelper;

    private static final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature())
            .build();

    private final ScimUserService userService;

    @Inject
    public ScimUsersResource(ScimUserService userService) {
        this.userService = userService;
    }

    /**
     * GET /scim/v2/Users/{id}
     * Hent en enkelt bruker
     */
    @GET
    @Path("/{id}")
    public Response getUser(@PathParam("id") String id) {
        log.debug("GET User: id={}", id);
        long startTid = System.currentTimeMillis();

        Optional<ScimUser> user = userService.getUser(id);

        if (user.isEmpty()) {
            long kalltid = System.currentTimeMillis() - startTid;
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users/" + id,
                    Response.Status.NOT_FOUND.getStatusCode(), kalltid, null, "User not found");
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"detail\":\"User not found: " + id + "\"}")
                .build();
        }

        long kalltid = System.currentTimeMillis() - startTid;
        String responseJson = toJson(user.get());
        kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users/" + id,
                Response.Status.OK.getStatusCode(), kalltid, responseJson, null);

        return Response.ok(user.get()).build();
    }

    /**
     * GET /scim/v2/Users?startIndex={n}&count={m}
     * List alle brukere (paginert)
     */
    @GET
    public Response listUsers(
            @QueryParam("startIndex") @DefaultValue("1") int startIndex,
            @QueryParam("count") @DefaultValue("100") int count,
            @QueryParam("filter") String filter) {

        log.debug("LIST Users: startIndex={}, count={}, filter={}", startIndex, count, filter);
        long startTid = System.currentTimeMillis();

        if (filter != null) {
            log.warn("Filter not implemented yet: {}", filter);
        }

        Page<ScimUser> userPage = userService.getUsers(startIndex, count);

        // Build SCIM ListResponse manually
        ScimListResponse<ScimUser> response = new ScimListResponse<>();
        response.setSchemas(java.util.List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"));
        response.setTotalResults((int) userPage.getTotalElements());
        response.setStartIndex(startIndex);
        response.setItemsPerPage(userPage.getNumberOfElements());
        response.setResources(userPage.getContent());

        long kalltid = System.currentTimeMillis() - startTid;
        kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users",
                Response.Status.OK.getStatusCode(), kalltid, toJson(response),
                "totalResults=" + response.getTotalResults());

        return Response.ok(response).build();
    }

    /**
     * POST /scim/v2/Users
     */
    @POST
    public Response createUser(ScimUser user) {
        log.info("CREATE User: userName={}", user != null ? user.getUserName() : null);

        String userJson;
        try {
            userJson = objectMapper.writeValueAsString(user);
        } catch (JsonProcessingException e) {
            log.error("Feil ved serialisering av bruker til JSON", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"detail\":\"Ugyldig bruker-data: " + e.getMessage() + "\"}")
                    .build();
        }

        long startTid = System.currentTimeMillis();
        PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(
                PLSQL_PROCEDURE_NAME,
                userJson
        );
        long kalltid = System.currentTimeMillis() - startTid;

        log.info("CREATE User fullført: messageNumber={}, message={}", result.getMessageNumber(), result.getMessage());

        kallLoggHelper.logg(KallLogg.METHOD_POST, "/scim/v2/Users",
                result.getMessageNumber(), kalltid, userJson, result.getData(), result.getMessage());

        return Response.status(Response.Status.CREATED)
                .entity(result.getData())
                .build();
    }


    /**
     * PUT /scim/v2/Users/{id}
     */
    @PUT
    @Path("/{id}")
    public Response updateUser(@PathParam("id") String id, ScimUser user) {
        log.info("UPDATE User: id={}", id);

        String userJson;
        try {
            userJson = objectMapper.writeValueAsString(user);
        } catch (JsonProcessingException e) {
            log.error("Feil ved serialisering av bruker til JSON", e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"detail\":\"Ugyldig bruker-data: " + e.getMessage() + "\"}")
                    .build();
        }

        long startTid = System.currentTimeMillis();
        PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(PLSQL_PROCEDURE_NAME, userJson);
        long kalltid = System.currentTimeMillis() - startTid;

        log.info("UPDATE User fullført: messageNumber={}, message={}", result.getMessageNumber(), result.getMessage());

        kallLoggHelper.logg(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id,
                result.getMessageNumber(), kalltid, userJson, result.getData(), result.getMessage());

        return Response.ok(result.getData()).build();
    }

    /**
     * DELETE /scim/v2/Users/{id}
     */
    @DELETE
    @Path("/{id}")
    public Response deleteUser(@PathParam("id") String id) {
        log.info("DELETE User: id={}", id);

        long startTid = System.currentTimeMillis();
        PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(PLSQL_PROCEDURE_NAME, id);
        long kalltid = System.currentTimeMillis() - startTid;

        log.info("DELETE User fullført: messageNumber={}, message={}", result.getMessageNumber(), result.getMessage());

        kallLoggHelper.logg(KallLogg.METHOD_DELETE, "/scim/v2/Users/" + id,
                result.getMessageNumber(), kalltid, id, result.getData(), result.getMessage());

        return Response.noContent().build();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Kunne ikke serialisere response til JSON for logging", e);
            return null;
        }
    }
}
