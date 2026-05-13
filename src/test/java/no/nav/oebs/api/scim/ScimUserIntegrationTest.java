package no.nav.oebs.api.scim;

import no.nav.oebs.api.Application;
import no.nav.oebs.api.db.repository.KallLoggRepository;
import no.nav.oebs.api.db.repository.KallLoggRepositoryImpl;
import no.nav.oebs.api.db.repository.PlsqlProcedureRepository;
import no.nav.oebs.api.db.repository.PlsqlProcedureResult;
import no.nav.oebs.api.scim.repository.ScimGroupMembershipRepository;
import no.nav.oebs.api.scim.repository.ScimGroupRepository;
import no.nav.oebs.api.scim.repository.ScimUserRepository;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integrasjonstester for SCIM User-endepunkter.
 * Starter en lokal MockOAuth2Server som utsteder ekte JWT-tokens,
 * og mocker alle DB-lag med @MockitoBean.
 *
 * Tester:
 *  - Sikkerhet: 401 uten token
 *  - GET /scim/v2/Users (liste)
 *  - GET /scim/v2/Users/{id} (funnet / ikke funnet)
 *  - POST /scim/v2/Users
 *  - PUT  /scim/v2/Users/{id}
 *  - DELETE /scim/v2/Users/{id}
 */
@SpringBootTest(
    classes = Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.autoconfigure.exclude=" +
            "org.apache.directory.scim.spring.ScimpleSpringConfiguration," +
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
            "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration," +
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration," +
            "org.springframework.boot.data.autoconfigure.web.DataWebAutoConfiguration",
        "spring.jpa.open-in-view=false"
    }
)
@ActiveProfiles("test")
class ScimUserIntegrationTest {

    static final MockOAuth2Server mockOAuth2Server;
    static final String TEST_ISSUER   = "azure";
    static final String TEST_AUDIENCE = "test-audience";

    static {
        mockOAuth2Server = new MockOAuth2Server();
        mockOAuth2Server.start();
    }

    @DynamicPropertySource
    static void configureOAuth(DynamicPropertyRegistry registry) {
        registry.add("no.nav.security.jwt.issuer.azure.discovery-url",
                () -> mockOAuth2Server.wellKnownUrl(TEST_ISSUER).toString());
        registry.add("no.nav.security.jwt.issuer.azure.accepted_audience",
                () -> TEST_AUDIENCE);
    }

    @AfterAll
    static void stopMockServer() {
        mockOAuth2Server.shutdown();
    }

    @MockitoBean KallLoggRepository             kallLoggRepository;
    @MockitoBean KallLoggRepositoryImpl         kallLoggRepositoryImpl;
    @MockitoBean PlsqlProcedureRepository       plsqlRepository;
    @MockitoBean ScimUserRepository             userRepository;
    @MockitoBean ScimGroupRepository            groupRepository;
    @MockitoBean ScimGroupMembershipRepository  groupMembershipRepository;

    @LocalServerPort
    int port;

    WebTestClient webTestClient;

    @BeforeEach
    void setupWebTestClient() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    private String issueToken() {
        return mockOAuth2Server
                .issueToken(TEST_ISSUER, "test-client", TEST_AUDIENCE)
                .serialize();
    }

    private ScimUserEntity enBrukerEntity() {
        ScimUserEntity e = new ScimUserEntity();
        e.setNavId("S123456");
        e.setBrukerId("ABC1234");
        e.setForNavn("Ola");
        e.setEtterNavn("Nordmann");
        e.setNavEPost("ola@nav.no");
        e.setActiveFlag("Y");
        e.setEnhetsId("1234");
        e.setArbeidsstedFylke("OSLO");
        return e;
    }

    @Test
    void getUsers_utenToken_gir401() {
        webTestClient.get().uri("/scim/v2/Users")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getUser_utenToken_gir401() {
        webTestClient.get().uri("/scim/v2/Users/S123456")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getUsers_medToken_gir200MedListResponse() {
        when(userRepository.findAllActiveUsers(any()))
                .thenReturn(new PageImpl<>(List.of(enBrukerEntity())));
        when(groupMembershipRepository.findByNavId(any())).thenReturn(List.of());

        webTestClient.get().uri("/scim/v2/Users")
                .header("Authorization", "Bearer " + issueToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> {
                    assertThat(body).contains("ListResponse");
                    assertThat(body).contains("totalResults");
                });
    }

    @Test
    void getUsers_tomSide_gir200MedTotalResults0() {
        when(userRepository.findAllActiveUsers(any())).thenReturn(Page.empty());

        webTestClient.get().uri("/scim/v2/Users")
                .header("Authorization", "Bearer " + issueToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body ->
                        assertThat(body).contains("\"totalResults\":0"));
    }

    @Test
    void getUser_finnes_gir200MedBruker() {
        when(userRepository.findByNavId("S123456")).thenReturn(Optional.of(enBrukerEntity()));
        when(groupMembershipRepository.findByNavId("S123456")).thenReturn(List.of());

        webTestClient.get().uri("/scim/v2/Users/S123456")
                .header("Authorization", "Bearer " + issueToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).value(body -> {
                    assertThat(body).contains("ABC1234");
                    assertThat(body).contains("Ola");
                });
    }

    @Test
    void getUser_finnesIkke_gir404() {
        when(userRepository.findByNavId("UKJENT")).thenReturn(Optional.empty());

        webTestClient.get().uri("/scim/v2/Users/UKJENT")
                .header("Authorization", "Bearer " + issueToken())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void createUser_medToken_gir201() {
        when(plsqlRepository.executeInOutProcedure(any(), any(), any()))
                .thenReturn(new PlsqlProcedureResult(null, 0, null, null, "0"));

        String body = """
            {
              "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
              "id": "S123456",
              "userName": "ABC1234",
              "active": true,
              "name": { "givenName": "Ola", "familyName": "Nordmann" }
            }
            """;

        webTestClient.post().uri("/scim/v2/Users")
                .header("Authorization", "Bearer " + issueToken())
                .contentType(MediaType.parseMediaType("application/scim+json"))
                .bodyValue(body)
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void updateUser_medToken_gir200() {
        when(plsqlRepository.executeInOutProcedure(any(), any(), any()))
                .thenReturn(new PlsqlProcedureResult(null, 0, null, null, "0"));

        String body = """
            {
              "schemas": ["urn:ietf:params:scim:schemas:core:2.0:User"],
              "id": "S123456",
              "userName": "ABC1234",
              "active": true,
              "name": { "givenName": "Ola", "familyName": "Nordmann" }
            }
            """;

        webTestClient.put().uri("/scim/v2/Users/S123456")
                .header("Authorization", "Bearer " + issueToken())
                .contentType(MediaType.parseMediaType("application/scim+json"))
                .bodyValue(body)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void deleteUser_medToken_gir204() {
        when(plsqlRepository.executeInOutProcedure(any(), any(), any()))
                .thenReturn(new PlsqlProcedureResult(null, 0, null, null, "0"));

        webTestClient.delete().uri("/scim/v2/Users/S123456")
                .header("Authorization", "Bearer " + issueToken())
                .exchange()
                .expectStatus().isNoContent();
    }
}

