package no.nav.oebs.api.scim.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import no.nav.oebs.api.scim.service.ScimGroupService;
import org.apache.directory.scim.spec.resources.ScimGroup;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * JAX-RS Resource for SCIM 2.0 Groups endpoint
 * GET /scim/v2/Groups
 * GET /scim/v2/Groups/{id}
 */
@Slf4j
@Component
@Path("/Groups")
@Produces("application/scim+json")
@Consumes("application/scim+json")
public class ScimGroupsResource {

    private final ScimGroupService groupService;

    @Inject
    public ScimGroupsResource(ScimGroupService groupService) {
        this.groupService = groupService;
    }

    /**
     * GET /scim/v2/Groups/{id}
     * Hent en enkelt gruppe eller ansvarsområde
     */
    @GET
    @Path("/{id}")
    public Response getGroup(@PathParam("id") String id) {
        log.debug("GET Group: id={}", id);

        Optional<ScimGroup> group = groupService.getGroup(id);

        if (group.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"detail\":\"Group not found: " + id + "\"}")
                .build();
        }

        return Response.ok(group.get()).build();
    }

    /**
     * GET /scim/v2/Groups?startIndex={n}&count={m}
     * List alle grupper og ansvarsområder (paginert)
     */
    @GET
    public Response listGroups(
            @QueryParam("startIndex") @DefaultValue("1") int startIndex,
            @QueryParam("count") @DefaultValue("100") int count,
            @QueryParam("filter") String filter) {

        log.debug("LIST Groups: startIndex={}, count={}, filter={}", startIndex, count, filter);

        if (filter != null) {
            log.warn("Filter not implemented yet: {}", filter);
        }

        Page<ScimGroup> groupPage = groupService.getGroups(startIndex, count);

        // Build SCIM ListResponse manually
        ScimListResponse<ScimGroup> response = new ScimListResponse<>();
        response.setSchemas(java.util.List.of("urn:ietf:params:scim:api:messages:2.0:ListResponse"));
        response.setTotalResults((int) groupPage.getTotalElements());
        response.setStartIndex(startIndex);
        response.setItemsPerPage(groupPage.getNumberOfElements());
        response.setResources(groupPage.getContent());

        return Response.ok(response).build();
    }

    /**
     * POST /scim/v2/Groups - NOT SUPPORTED (read-only)
     */
    @POST
    public Response createGroup(ScimGroup group) {
        log.warn("CREATE Group - not supported (read-only API)");
        return Response.status(Response.Status.NOT_IMPLEMENTED)
            .entity("{\"detail\":\"Group creation not supported - read-only API\"}")
            .build();
    }

    /**
     * PUT /scim/v2/Groups/{id} - NOT SUPPORTED (read-only)
     */
    @PUT
    @Path("/{id}")
    public Response updateGroup(@PathParam("id") String id, ScimGroup group) {
        log.warn("UPDATE Group {} - not supported (read-only API)", id);
        return Response.status(Response.Status.NOT_IMPLEMENTED)
            .entity("{\"detail\":\"Group update not supported - read-only API\"}")
            .build();
    }

    /**
     * DELETE /scim/v2/Groups/{id} - NOT SUPPORTED (read-only)
     */
    @DELETE
    @Path("/{id}")
    public Response deleteGroup(@PathParam("id") String id) {
        log.warn("DELETE Group {} - not supported (read-only API)", id);
        return Response.status(Response.Status.NOT_IMPLEMENTED)
            .entity("{\"detail\":\"Group deletion not supported - read-only API\"}")
            .build();
    }
}


