package no.nav.oebs.api.scim.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
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
@Path("/scim/v2/Users")
@Produces("application/scim+json")
@Consumes("application/scim+json")
public class ScimUsersResource {

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

        Optional<ScimUser> user = userService.getUser(id);

        if (user.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"detail\":\"User not found: " + id + "\"}")
                .build();
        }

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

        return Response.ok(response).build();
    }

    /**
     * POST /scim/v2/Users - NOT SUPPORTED (read-only)
     */
    @POST
    public Response createUser(ScimUser user) {
        log.warn("CREATE User - not supported (read-only API)");
        return Response.status(Response.Status.NOT_IMPLEMENTED)
            .entity("{\"detail\":\"User creation not supported - read-only API\"}")
            .build();
    }

    /**
     * PUT /scim/v2/Users/{id} - NOT SUPPORTED (read-only)
     */
    @PUT
    @Path("/{id}")
    public Response updateUser(@PathParam("id") String id, ScimUser user) {
        log.warn("UPDATE User {} - not supported (read-only API)", id);
        return Response.status(Response.Status.NOT_IMPLEMENTED)
            .entity("{\"detail\":\"User update not supported - read-only API\"}")
            .build();
    }

    /**
     * DELETE /scim/v2/Users/{id} - NOT SUPPORTED (read-only)
     */
    @DELETE
    @Path("/{id}")
    public Response deleteUser(@PathParam("id") String id) {
        log.warn("DELETE User {} - not supported (read-only API)", id);
        return Response.status(Response.Status.NOT_IMPLEMENTED)
            .entity("{\"detail\":\"User deletion not supported - read-only API\"}")
            .build();
    }
}


