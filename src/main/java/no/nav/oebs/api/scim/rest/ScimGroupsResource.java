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
import no.nav.oebs.api.scim.service.ScimGroupService;
import org.apache.directory.scim.spec.resources.ScimGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JAX-RS Resource for SCIM 2.0 Groups endpoint (read-only)
 * GET /scim/v2/Groups
 * GET /scim/v2/Groups/{id}
 */
@Slf4j
@Component
@Path("/Groups")
@Produces("application/scim+json")
@Consumes("application/scim+json")
public class ScimGroupsResource {

    @Autowired
    private KallLoggHelper kallLoggHelper;

    private final ScimGroupService groupService;

    private static final ObjectMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .enable(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature())
            .build();

    @Inject
    public ScimGroupsResource(ScimGroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * GET /scim/v2/Groups/{id}
     */
    @GET
    @Path("/{id}")
    public Response getGroup(@PathParam("id") String id) {
        log.debug("GET Group: id={}", id);
        long startTid = System.currentTimeMillis();

        Optional<ScimGroup> group = groupService.getGroup(id);

        if (group.isEmpty()) {
            long kalltid = System.currentTimeMillis() - startTid;
            kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Groups/" + id,
                    Response.Status.NOT_FOUND.getStatusCode(), kalltid, null, "Group not found");
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"detail\":\"Group not found: " + id + "\"}")
                .build();
        }

        long kalltid = System.currentTimeMillis() - startTid;
        String responseJson = toJson(group.get());
        kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Groups/" + id,
                Response.Status.OK.getStatusCode(), kalltid, responseJson, null);

        return Response.ok(group.get()).build();
    }

    /**
     * GET /scim/v2/Groups?startIndex={n}&count={m}
     */
    @GET
    public Response listGroups(
            @QueryParam("startIndex") @DefaultValue("1") int startIndex,
            @QueryParam("count") @DefaultValue("100") int count,
            @QueryParam("filter") String filter) {

        log.debug("LIST Groups: startIndex={}, count={}, filter={}", startIndex, count, filter);
        long startTid = System.currentTimeMillis();

        if (filter != null) {
            log.warn("Filter not implemented yet: {}", filter);
        }

        Page<ScimGroup> groupPage = groupService.getGroups(startIndex, count);

        ScimListResponse<ScimGroup> response = new ScimListResponse<>();
        response.setSchemas(java.util.List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"));
        response.setTotalResults((int) groupPage.getTotalElements());
        response.setStartIndex(startIndex);
        response.setItemsPerPage(groupPage.getNumberOfElements());
        response.setResources(groupPage.getContent());

        long kalltid = System.currentTimeMillis() - startTid;
        kallLoggHelper.loggInn(KallLogg.METHOD_GET, "/scim/v2/Groups",
                Response.Status.OK.getStatusCode(), kalltid, toJson(response),
                "totalResults=" + response.getTotalResults());

        return Response.ok(response).build();
    }

    /**
     * POST /scim/v2/Groups - ikke støttet, grupper er read-only
     */
    @POST
    public Response createGroup(ScimGroup group) {
        log.warn("CREATE Group - ikke tillatt (read-only)");
        kallLoggHelper.loggUt(KallLogg.METHOD_POST, "/scim/v2/Groups",
                Response.Status.METHOD_NOT_ALLOWED.getStatusCode(), 0, null, null, "Read-only - ikke støttet");
        return Response.status(Response.Status.METHOD_NOT_ALLOWED)
                .entity("{\"detail\":\"Groups er read-only - POST ikke støttet\"}")
                .build();
    }

    /**
     * PUT /scim/v2/Groups/{id} - ikke støttet, grupper er read-only
     */
    @PUT
    @Path("/{id}")
    public Response updateGroup(@PathParam("id") String id, ScimGroup group) {
        log.warn("UPDATE Group {} - ikke tillatt (read-only)", id);
        kallLoggHelper.loggUt(KallLogg.METHOD_PUT, "/scim/v2/Groups/" + id,
                Response.Status.METHOD_NOT_ALLOWED.getStatusCode(), 0, null, null, "Read-only - ikke støttet");
        return Response.status(Response.Status.METHOD_NOT_ALLOWED)
                .entity("{\"detail\":\"Groups er read-only - PUT ikke støttet\"}")
                .build();
    }

    /**
     * DELETE /scim/v2/Groups/{id} - ikke støttet, grupper er read-only
     */
    @DELETE
    @Path("/{id}")
    public Response deleteGroup(@PathParam("id") String id) {
        log.warn("DELETE Group {} - ikke tillatt (read-only)", id);
        kallLoggHelper.loggUt(KallLogg.METHOD_DELETE, "/scim/v2/Groups/" + id,
                Response.Status.METHOD_NOT_ALLOWED.getStatusCode(), 0, null, null, "Read-only - ikke støttet");
        return Response.status(Response.Status.METHOD_NOT_ALLOWED)
                .entity("{\"detail\":\"Groups er read-only - DELETE ikke støttet\"}")
                .build();
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
