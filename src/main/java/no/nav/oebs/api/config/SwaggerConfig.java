package no.nav.oebs.api.config;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    public static final String BEARER_TOKEN_AUTH = "BearerToken";

    @Value("${OEBS_ENV}")
    String env;

    @Value("${APP_UPDATE}")
    String dato;

    @Value("${APP_VERSION}")
    String versjon;

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title(env + " – OeBS Omada SCIM API")
                        .description("""
                                <p>SCIM 2.0 API for synkronisering av brukere og grupper mellom OeBS og Omada.</p>
                                <p><b>Sikkerhet:</b> Alle endepunkter bortsett fra <code>/Schemas</code>,
                                <code>/ResourceTypes</code> og <code>/ServiceProviderConfig</code> krever
                                gyldig Azure AD Bearer-token.</p>
                                """)
                        .version(versjon + " (" + dato + ")"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_TOKEN_AUTH,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Azure AD aksesstoken — lim inn uten 'Bearer' foran."))
                        .addSchemas("ScimUser", scimUserSchema())
                        .addSchemas("ScimGroup", scimGroupSchema())
                        .addSchemas("ScimListResponse", scimListResponseSchema())
                        .addSchemas("ScimError", scimErrorSchema())
                        .addSchemas("EnterpriseExtension", enterpriseExtensionSchema())
                        .addSchemas("NavOebsExtension", navOebsExtensionSchema()))
                .security(List.of(new SecurityRequirement().addList(BEARER_TOKEN_AUTH)))
                .paths(scimPaths());
    }

    // ── Paths ────────────────────────────────────────────────────────────────

    private Paths scimPaths() {
        Paths paths = new Paths();

        // Users
        paths.addPathItem("/scim/v2/Users", usersCollectionPath());
        paths.addPathItem("/scim/v2/Users/{id}", userItemPath());

        // Groups
        paths.addPathItem("/scim/v2/Groups", groupsCollectionPath());
        paths.addPathItem("/scim/v2/Groups/{id}", groupItemPath());

        // Metadata (åpne, ingen token)
        paths.addPathItem("/scim/v2/ServiceProviderConfig", metaPath("ServiceProviderConfig", "SCIM ServiceProvider-konfigurasjon"));
        paths.addPathItem("/scim/v2/Schemas", metaPath("Schemas", "Alle registrerte SCIM-skjemaer"));
        paths.addPathItem("/scim/v2/ResourceTypes", metaPath("ResourceTypes", "Alle registrerte SCIM-ressurstyper"));

        return paths;
    }

    // ── /scim/v2/Users ───────────────────────────────────────────────────────

    private PathItem usersCollectionPath() {
        return new PathItem()
                .get(new Operation()
                        .summary("Hent alle aktive brukere (paginert)")
                        .tags(List.of("Users"))
                        .addParametersItem(startIndexParam())
                        .addParametersItem(countParam())
                        .addParametersItem(filterParam())
                        .responses(new ApiResponses()
                                .addApiResponse("200", listResponse("ScimUser"))
                                .addApiResponse("401", errorResponse("Manglende eller ugyldig token"))))
                .post(new Operation()
                        .summary("Opprett bruker i OeBS")
                        .tags(List.of("Users"))
                        .requestBody(scimRequestBody("ScimUser", "SCIM User-objekt"))
                        .responses(new ApiResponses()
                                .addApiResponse("201", singleResponse("ScimUser", "Bruker opprettet"))
                                .addApiResponse("202", errorResponse("Synkronisering akseptert og pågår"))
                                .addApiResponse("400", errorResponse("Ugyldig forespørsel"))
                                .addApiResponse("401", errorResponse("Manglende eller ugyldig token"))
                                .addApiResponse("500", errorResponse("Intern feil / prosedyrefeil"))));
    }

    // ── /scim/v2/Users/{id} ──────────────────────────────────────────────────

    private PathItem userItemPath() {
        return new PathItem()
                .get(new Operation()
                        .summary("Hent enkelt bruker")
                        .tags(List.of("Users"))
                        .addParametersItem(idParam("nav-id / externalId, f.eks. K105317"))
                        .responses(new ApiResponses()
                                .addApiResponse("200", singleResponse("ScimUser", "Bruker funnet"))
                                .addApiResponse("401", errorResponse("Manglende eller ugyldig token"))
                                .addApiResponse("404", errorResponse("Bruker ikke funnet"))))
                .put(new Operation()
                        .summary("Oppdater bruker i OeBS")
                        .tags(List.of("Users"))
                        .addParametersItem(idParam("nav-id / externalId"))
                        .requestBody(scimRequestBody("ScimUser", "SCIM User-objekt (fullstendig)"))
                        .responses(new ApiResponses()
                                .addApiResponse("200", singleResponse("ScimUser", "Bruker oppdatert"))
                                .addApiResponse("202", errorResponse("Synkronisering akseptert og pågår"))
                                .addApiResponse("400", errorResponse("Ugyldig forespørsel"))
                                .addApiResponse("401", errorResponse("Manglende eller ugyldig token"))
                                .addApiResponse("422", errorResponse("Advarsel fra sync-prosedyre"))
                                .addApiResponse("500", errorResponse("Intern feil / prosedyrefeil"))))
                .delete(new Operation()
                        .summary("Slett bruker i OeBS")
                        .tags(List.of("Users"))
                        .addParametersItem(idParam("nav-id / externalId"))
                        .responses(new ApiResponses()
                                .addApiResponse("204", new ApiResponse().description("Bruker slettet"))
                                .addApiResponse("401", errorResponse("Manglende eller ugyldig token"))
                                .addApiResponse("500", errorResponse("Intern feil / prosedyrefeil"))));
    }

    // ── /scim/v2/Groups ──────────────────────────────────────────────────────

    private PathItem groupsCollectionPath() {
        return new PathItem()
                .get(new Operation()
                        .summary("Hent alle aktive grupper og ansvarsområder (paginert)")
                        .tags(List.of("Groups"))
                        .addParametersItem(startIndexParam())
                        .addParametersItem(countParam())
                        .addParametersItem(filterParam())
                        .responses(new ApiResponses()
                                .addApiResponse("200", listResponse("ScimGroup"))
                                .addApiResponse("401", errorResponse("Manglende eller ugyldig token"))));
    }

    private PathItem groupItemPath() {
        return new PathItem()
                .get(new Operation()
                        .summary("Hent enkelt gruppe/ansvarsområde")
                        .description("""
                                ID-format:
                                - Grupper: <code>G&lt;group_id&gt;</code> (f.eks. <code>G12345</code>)
                                - Ansvarsområder: <code>A&lt;responsibility_id&gt;</code> (f.eks. <code>A51840</code>)
                                """)
                        .tags(List.of("Groups"))
                        .addParametersItem(idParam("SCIM-gruppe-ID, f.eks. G12345 eller A51840"))
                        .responses(new ApiResponses()
                                .addApiResponse("200", singleResponse("ScimGroup", "Gruppe funnet"))
                                .addApiResponse("401", errorResponse("Manglende eller ugyldig token"))
                                .addApiResponse("404", errorResponse("Gruppe ikke funnet"))));
    }

    // ── Metadata (åpne endepunkter) ───────────────────────────────────────────

    private PathItem metaPath(String tag, String summary) {
        return new PathItem()
                .get(new Operation()
                        .summary(summary)
                        .tags(List.of("SCIM Metadata"))
                        .security(List.of())   // åpent — ingen token påkrevd
                        .responses(new ApiResponses()
                                .addApiResponse("200", new ApiResponse()
                                        .description("OK")
                                        .content(new Content().addMediaType("application/scim+json",
                                                new MediaType().schema(new ObjectSchema()))))));
    }

    // ── Schemas ───────────────────────────────────────────────────────────────

    private Schema<?> scimUserSchema() {
        return new ObjectSchema()
                .description("SCIM 2.0 User-objekt med Enterprise- og NAV OeBS-extension")
                .addProperty("schemas", new ArraySchema().items(new StringSchema())
                        .example(List.of("urn:ietf:params:scim:schemas:core:2.0:User",
                                "urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                                "urn:ietf:params:scim:schemas:extension:nav:oebs:2.0:User")))
                .addProperty("id", new StringSchema().description("SCIM id — nav-id (externalId), f.eks. K105317"))
                .addProperty("externalId", new StringSchema().description("NAV-id utledet fra e-postadresse, f.eks. K105317"))
                .addProperty("userName", new StringSchema().description("OeBS brukernavn (3 bokstaver + 4 siffer), f.eks. ABC1234"))
                .addProperty("active", new BooleanSchema().description("Om brukerens konto er aktiv i OeBS"))
                .addProperty("name", new ObjectSchema()
                        .addProperty("givenName", new StringSchema().description("Fornavn"))
                        .addProperty("familyName", new StringSchema().description("Etternavn")))
                .addProperty("emails", new ArraySchema().items(new ObjectSchema()
                        .addProperty("value", new StringSchema().description("E-postadresse"))
                        .addProperty("type", new StringSchema()._enum(List.of("work")))
                        .addProperty("primary", new BooleanSchema())))
                .addProperty("groups", new ArraySchema().items(new ObjectSchema()
                        .addProperty("value", new StringSchema().description("SCIM gruppe-ID"))
                        .addProperty("display", new StringSchema().description("Gruppenavn"))))
                .addProperty("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User",
                        new Schema<>().$ref("#/components/schemas/EnterpriseExtension"))
                .addProperty("urn:ietf:params:scim:schemas:extension:nav:oebs:2.0:User",
                        new Schema<>().$ref("#/components/schemas/NavOebsExtension"));
    }

    private Schema<?> enterpriseExtensionSchema() {
        return new ObjectSchema()
                .description("SCIM Enterprise extension")
                .addProperty("department", new StringSchema().description("Enhets-ID — siste 4 siffer av userName"))
                .addProperty("division", new StringSchema().description("Arbeidssted/fylke — location_code fra HR_LOCATIONS"));
    }

    private Schema<?> navOebsExtensionSchema() {
        return new ObjectSchema()
                .description("NAV OeBS-spesifikk extension (urn:ietf:params:scim:schemas:extension:nav:oebs:2.0:User)")
                .addProperty("fullmakt", new StringSchema().description("Fullmakt-tittel fra per_job_definitions"))
                .addProperty("egenansatt", new BooleanSchema().description("Skjermingsflagg — true hvis brukeren er registrert i XXRTV_SKJERMING_TILGANG"))
                .addProperty("nyttPassord", new BooleanSchema().description("Sett true for å generere nytt passord (WRITE_ONLY — returneres aldri i GET)"));
    }

    private Schema<?> scimGroupSchema() {
        return new ObjectSchema()
                .description("SCIM 2.0 Group-objekt — representerer både JTF-grupper (G$) og ansvarsområder (A$)")
                .addProperty("schemas", new ArraySchema().items(new StringSchema()))
                .addProperty("id", new StringSchema().description("SCIM-ID — format G<group_id> eller A<responsibility_id>"))
                .addProperty("externalId", new StringSchema().description("OeBS-intern kilde-ID"))
                .addProperty("displayName", new StringSchema().description("Gruppenavn"))
                .addProperty("members", new ArraySchema().items(new ObjectSchema()
                        .addProperty("value", new StringSchema().description("nav-id til brukermedlem"))
                        .addProperty("display", new StringSchema().description("Brukernavn"))));
    }

    private Schema<?> scimListResponseSchema() {
        return new ObjectSchema()
                .description("SCIM 2.0 ListResponse")
                .addProperty("schemas", new ArraySchema().items(new StringSchema()))
                .addProperty("totalResults", new IntegerSchema().description("Totalt antall treff"))
                .addProperty("startIndex", new IntegerSchema().description("1-basert startindeks"))
                .addProperty("itemsPerPage", new IntegerSchema().description("Antall returnerte objekter"))
                .addProperty("Resources", new ArraySchema().items(new ObjectSchema()));
    }

    private Schema<?> scimErrorSchema() {
        return new ObjectSchema()
                .description("SCIM 2.0 Error-respons")
                .addProperty("schemas", new ArraySchema().items(new StringSchema()))
                .addProperty("status", new StringSchema().description("HTTP-statuskode som streng"))
                .addProperty("detail", new StringSchema().description("Feilbeskrivelse"));
    }

    // ── Hjelpemetoder ─────────────────────────────────────────────────────────

    private Parameter idParam(String description) {
        return new Parameter().name("id").in("path").required(true)
                .description(description)
                .schema(new StringSchema());
    }

    private Parameter startIndexParam() {
        return new Parameter().name("startIndex").in("query").required(false)
                .description("1-basert startindeks for paginering (default: 1)")
                .schema(new IntegerSchema()._default(1));
    }

    private Parameter countParam() {
        return new Parameter().name("count").in("query").required(false)
                .description("Maks antall resultater å returnere (default: 100)")
                .schema(new IntegerSchema()._default(100));
    }

    private Parameter filterParam() {
        return new Parameter().name("filter").in("query").required(false)
                .description("SCIM filteruttrykk, f.eks. userName eq \"ABC1234\"")
                .schema(new StringSchema());
    }

    private RequestBody scimRequestBody(String schemaRef, String description) {
        return new RequestBody()
                .description(description)
                .required(true)
                .content(new Content()
                        .addMediaType("application/scim+json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/" + schemaRef)))
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/" + schemaRef))));
    }

    private ApiResponse listResponse(String schemaRef) {
        return new ApiResponse()
                .description("Liste med " + schemaRef + "-objekter (SCIM ListResponse)")
                .content(new Content()
                        .addMediaType("application/scim+json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ScimListResponse"))));
    }

    private ApiResponse singleResponse(String schemaRef, String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/scim+json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/" + schemaRef))));
    }

    private ApiResponse errorResponse(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/scim+json", new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/ScimError"))));
    }
}
