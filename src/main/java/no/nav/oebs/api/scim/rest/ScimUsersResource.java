package no.nav.oebs.api.scim.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.common.swagger.OmadaSwagger;
import no.nav.oebs.api.db.entity.KallLogg;
import no.nav.oebs.api.db.repository.PlsqlProcedureRepository;
import no.nav.oebs.api.db.repository.PlsqlProcedureResult;
import no.nav.oebs.api.scim.service.ScimUserService;
import org.apache.directory.scim.spec.resources.ScimUser;
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
@Produces({"application/scim+json", "application/json"})
@Consumes({"application/scim+json", "application/json"})
public class ScimUsersResource {

    private static final String PLSQL_PROCEDURE_NAME = "XXRTV_INT_OMADA_INSERT_MESSAGE.InsertOmadaMessage";
    private static final String OPERASJON_NY     = "NY";
    private static final String OPERASJON_ENDRE  = "ENDRE";
    private static final String OPERASJON_SLETT  = "SLETT";

    private static final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature())
            .build();

    private final PlsqlProcedureRepository plsqlRepository;
    private final KallLoggHelper kallLoggHelper;
    private final ScimUserService userService;

    @Inject
    public ScimUsersResource(PlsqlProcedureRepository plsqlRepository,
                             KallLoggHelper kallLoggHelper,
                             ScimUserService userService) {
        this.plsqlRepository = plsqlRepository;
        this.kallLoggHelper = kallLoggHelper;
        this.userService = userService;
    }

    /**
     * GET /scim/v2/Users/{id}
     */
    @GET
    @Path("/{id}")
    @OmadaSwagger
    public Response getUser(@PathParam("id") String id) {
        log.info("GET User: id={}", id);
        long startTid = System.currentTimeMillis();

        Optional<ScimUser> user = userService.getUser(id);

        if (user.isEmpty()) {
            long kalltid = System.currentTimeMillis() - startTid;
            log.info("GET User: id={} - IKKE FUNNET, kalltid={}ms", id, kalltid);
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users/" + id,
                    Response.Status.NOT_FOUND.getStatusCode(), kalltid, null, "User not found");
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"detail\":\"User not found: " + id + "\"}")
                .build();
        }

        long kalltid = System.currentTimeMillis() - startTid;
        String responseJson = toJson(user.get());
        log.info("GET User: id={} - FUNNET, kalltid={}ms", id, kalltid);
        kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users/" + id,
                Response.Status.OK.getStatusCode(), kalltid, responseJson, null);

        return Response.ok(user.get()).build();
    }

    /**
     * GET /scim/v2/Users?startIndex={n}&count={m}
     */
    @GET
    @OmadaSwagger
    public Response listUsers(
            @QueryParam("startIndex") @DefaultValue("1") int startIndex,
            @QueryParam("count") @DefaultValue("100") int count,
            @QueryParam("filter") String filter) {

        log.info("LIST Users: startIndex={}, count={}, filter={}", startIndex, count, filter);
        long startTid = System.currentTimeMillis();

        if (filter != null) {
            log.warn("Filter not implemented yet: {}", filter);
        }

        Page<ScimUser> userPage = userService.getUsers(startIndex, count);

        ScimListResponse<ScimUser> response = new ScimListResponse<>();
        response.setSchemas(java.util.List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"));
        response.setTotalResults((int) userPage.getTotalElements());
        response.setStartIndex(startIndex);
        response.setItemsPerPage(userPage.getNumberOfElements());
        response.setResources(userPage.getContent());

        long kalltid = System.currentTimeMillis() - startTid;
        log.info("LIST Users fullført: totalResults={}, returnert={}, kalltid={}ms",
                response.getTotalResults(), response.getItemsPerPage(), kalltid);
        kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Users",
                Response.Status.OK.getStatusCode(), kalltid, toJson(response),
                "totalResults=" + response.getTotalResults());

        return Response.ok(response).build();
    }

    /**
     * POST /scim/v2/Users
     */
    @POST
    @OmadaSwagger
    public Response createUser(String body) {
        log.info("CREATE User - innkommende request body: {}", body);

        Response validationError = validateBody(body, KallLogg.METHOD_POST, "/scim/v2/Users");
        if (validationError != null) return validationError;

        ScimUser user;
        try {
            user = objectMapper.readValue(body, ScimUser.class);
        } catch (JsonProcessingException e) {
            log.error("Feil ved deserialisering av SCIM JSON til ScimUser", e);
            String errorJson = "{\"detail\":\"Ugyldig SCIM bruker-data: " + e.getMessage() + "\"}";
            kallLoggHelper.loggUt(KallLogg.METHOD_POST, "/scim/v2/Users",
                    Response.Status.BAD_REQUEST.getStatusCode(), 0, body, errorJson, e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(errorJson).build();
        }

        if (user == null) {
            log.warn("CREATE User avvist: deserialisert bruker er null");
            String errorJson = "{\"detail\":\"Bruker-data er null\"}";
            kallLoggHelper.loggUt(KallLogg.METHOD_POST, "/scim/v2/Users",
                    Response.Status.BAD_REQUEST.getStatusCode(), 0, body, errorJson, "Bruker er null");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorJson).build();
        }

        if (user.getUserName() == null || user.getUserName().isBlank()) {
            log.warn("CREATE User avvist: userName er null eller tom");
            String errorJson = "{\"detail\":\"userName er påkrevd\"}";
            kallLoggHelper.loggUt(KallLogg.METHOD_POST, "/scim/v2/Users",
                    Response.Status.BAD_REQUEST.getStatusCode(), 0, body, errorJson, "userName mangler");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorJson).build();
        }

        log.info("CREATE User: userName={}, externalId={}, active={}",
                user.getUserName(), user.getExternalId(), user.getActive());

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
        PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(PLSQL_PROCEDURE_NAME, OPERASJON_NY, userJson);
        long kalltid = System.currentTimeMillis() - startTid;

        log.info("CREATE User fullført: messageNumber={}, message={}, kalltid={}ms",
                result.getMessageNumber(), result.getMessage(), kalltid);
        log.info("CREATE User svar: {}", result.getData());

        kallLoggHelper.loggUt(KallLogg.METHOD_POST, "/scim/v2/Users",
                result.getMessageNumber(), kalltid, userJson, result.getData(), result.getMessage());

        return Response.status(Response.Status.CREATED).entity(result.getData()).build();
    }

    /**
     * PUT /scim/v2/Users/{id}
     */
    @PUT
    @Path("/{id}")
    @OmadaSwagger
    public Response updateUser(@PathParam("id") String id, String body) {
        log.info("UPDATE User: id={} - innkommende request body: {}", id, body);

        Response validationError = validateBody(body, KallLogg.METHOD_PUT, "/scim/v2/Users/" + id);
        if (validationError != null) return validationError;

        ScimUser user;
        try {
            user = objectMapper.readValue(body, ScimUser.class);
        } catch (JsonProcessingException e) {
            log.error("Feil ved deserialisering av SCIM JSON til ScimUser", e);
            String errorJson = "{\"detail\":\"Ugyldig SCIM bruker-data: " + e.getMessage() + "\"}";
            kallLoggHelper.loggUt(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id,
                    Response.Status.BAD_REQUEST.getStatusCode(), 0, body, errorJson, e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST).entity(errorJson).build();
        }

        if (user == null) {
            log.warn("UPDATE User avvist: deserialisert bruker er null, id={}", id);
            String errorJson = "{\"detail\":\"Bruker-data er null\"}";
            kallLoggHelper.loggUt(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id,
                    Response.Status.BAD_REQUEST.getStatusCode(), 0, body, errorJson, "Bruker er null");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorJson).build();
        }

        if (user.getUserName() == null || user.getUserName().isBlank()) {
            log.warn("UPDATE User avvist: userName er null eller tom, id={}", id);
            String errorJson = "{\"detail\":\"userName er påkrevd\"}";
            kallLoggHelper.loggUt(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id,
                    Response.Status.BAD_REQUEST.getStatusCode(), 0, body, errorJson, "userName mangler");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorJson).build();
        }

        log.info("UPDATE User: id={}, userName={}, externalId={}, active={}",
                id, user.getUserName(), user.getExternalId(), user.getActive());

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
        PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(PLSQL_PROCEDURE_NAME, OPERASJON_ENDRE, userJson);
        long kalltid = System.currentTimeMillis() - startTid;

        log.info("UPDATE User fullført: id={}, messageNumber={}, message={}, kalltid={}ms",
                id, result.getMessageNumber(), result.getMessage(), kalltid);
        log.info("UPDATE User svar: {}", result.getData());

        kallLoggHelper.loggUt(KallLogg.METHOD_PUT, "/scim/v2/Users/" + id,
                result.getMessageNumber(), kalltid, userJson, result.getData(), result.getMessage());

        return Response.ok(result.getData()).build();
    }

    /**
     * DELETE /scim/v2/Users/{id}
     */
    @DELETE
    @Path("/{id}")
    @OmadaSwagger
    public Response deleteUser(@PathParam("id") String id) {
        log.info("DELETE User: id={}", id);

        if (id == null || id.isBlank()) {
            log.warn("DELETE User avvist: id er null eller tom");
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"detail\":\"id er påkrevd\"}")
                    .build();
        }

        long startTid = System.currentTimeMillis();
        PlsqlProcedureResult result = plsqlRepository.executeInOutProcedure(PLSQL_PROCEDURE_NAME, OPERASJON_SLETT, id);
        long kalltid = System.currentTimeMillis() - startTid;

        log.info("DELETE User fullført: id={}, messageNumber={}, message={}, kalltid={}ms",
                id, result.getMessageNumber(), result.getMessage(), kalltid);
        log.info("DELETE User svar: {}", result.getData());

        kallLoggHelper.loggUt(KallLogg.METHOD_DELETE, "/scim/v2/Users/" + id,
                result.getMessageNumber(), kalltid, id, result.getData(), result.getMessage());

        return Response.noContent().build();
    }

    /**
     * Felles validering av request body for POST og PUT.
     * Returnerer en BAD_REQUEST-respons om body er ugyldig, ellers null.
     */
    private Response validateBody(String body, String method, String operation) {
        if (body == null || body.isBlank()) {
            log.warn("{} avvist: request body er null eller tom", operation);
            String errorJson = "{\"detail\":\"Request body kan ikke være tom\"}";
            kallLoggHelper.loggUt(method, operation,
                    Response.Status.BAD_REQUEST.getStatusCode(), 0, null, errorJson, "Body er null/tom");
            return Response.status(Response.Status.BAD_REQUEST).entity(errorJson).build();
        }
        return null;
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
