package no.nav.oebs.api.scim;

import no.nav.oebs.api.Application;
import no.nav.oebs.api.db.repository.KallLoggRepository;
import no.nav.oebs.api.db.repository.KallLoggRepositoryImpl;
import no.nav.oebs.api.db.repository.PlsqlProcedureRepository;
import no.nav.oebs.api.scim.repository.ScimGroupMembershipRepository;
import no.nav.oebs.api.scim.repository.ScimGroupRepository;
import no.nav.oebs.api.scim.repository.ScimUserRepository;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
class ScimGroupIntegrationTest {

    static final MockOAuth2Server mockOAuth2Server;
    static final String TEST_ISSUER = "azure";
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

    @MockitoBean
    KallLoggRepository kallLoggRepository;
    @MockitoBean
    KallLoggRepositoryImpl kallLoggRepositoryImpl;
    @MockitoBean
    PlsqlProcedureRepository plsqlRepository;
    @MockitoBean
    ScimUserRepository userRepository;
    @MockitoBean
    ScimGroupRepository groupRepository;
    @MockitoBean
    ScimGroupMembershipRepository groupMembershipRepository;

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

    private ScimGroupEntity enGruppeEntity() {
        ScimGroupEntity e = new ScimGroupEntity();
        e.setScimId("G$1234");
        e.setScimDisplayName("NAV IT");
        e.setKildeType("G");
        e.setKildeId(1234L);
        return e;
    }

    @Test
    void getGroups_withoutToken_returns401() {
        webTestClient.get().uri("/scim/v2/Groups")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getGroup_withoutToken_returns401() {
        webTestClient.get().uri("/scim/v2/Groups/G$1234")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getGroups_withToken_returns200AndListResponse() {
        when(groupRepository.findAllByOrderByScimIdAsc(any()))
                .thenReturn(new PageImpl<>(List.of(enGruppeEntity())));

        webTestClient.get().uri("/scim/v2/Groups")
                .header("Authorization", "Bearer " + issueToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> {
                    assertThat(body).contains("ListResponse");
                    assertThat(body).contains("totalResults");
                    assertThat(body).contains("NAV IT");
                });
    }

    @Test
    void getGroup_found_returns200() {
        when(groupRepository.findByScimId("G$1234"))
                .thenReturn(Optional.of(enGruppeEntity()));
        when(groupMembershipRepository.findByScimGroupId("G$1234"))
                .thenReturn(List.of());

        webTestClient.get().uri("/scim/v2/Groups/G$1234")
                .header("Authorization", "Bearer " + issueToken())
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .value(body -> assertThat(body).contains("NAV IT"));
    }

    @Test
    void getGroup_notFound_returns404() {
        when(groupRepository.findByScimId("G$UKJENT"))
                .thenReturn(Optional.empty());

        webTestClient.get().uri("/scim/v2/Groups/G$UKJENT")
                .header("Authorization", "Bearer " + issueToken())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void postGroup_returns405() {
        webTestClient.post().uri("/scim/v2/Groups")
                .header("Authorization", "Bearer " + issueToken())
                .contentType(MediaType.parseMediaType("application/scim+json"))
                .bodyValue("{}")
                .exchange()
                .expectStatus().isEqualTo(405);
    }

    @Test
    void putGroup_returns405() {
        webTestClient.put().uri("/scim/v2/Groups/G$1234")
                .header("Authorization", "Bearer " + issueToken())
                .contentType(MediaType.parseMediaType("application/scim+json"))
                .bodyValue("{}")
                .exchange()
                .expectStatus().isEqualTo(405);
    }

    @Test
    void patchGroup_returns405() {
        webTestClient.patch().uri("/scim/v2/Groups/G$1234")
                .header("Authorization", "Bearer " + issueToken())
                .contentType(MediaType.parseMediaType("application/scim+json"))
                .bodyValue("{\"Operations\":[]}")
                .exchange()
                .expectStatus().isEqualTo(405);
    }

    @Test
    void deleteGroup_returns405() {
        webTestClient.delete().uri("/scim/v2/Groups/G$1234")
                .header("Authorization", "Bearer " + issueToken())
                .exchange()
                .expectStatus().isEqualTo(405);
    }
}

