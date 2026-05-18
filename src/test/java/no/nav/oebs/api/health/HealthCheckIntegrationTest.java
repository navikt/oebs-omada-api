package no.nav.oebs.api.health;

import no.nav.oebs.api.Application;
import no.nav.oebs.api.db.repository.KallLoggRepository;
import no.nav.oebs.api.db.repository.KallLoggRepositoryImpl;
import no.nav.oebs.api.db.repository.PlsqlProcedureRepository;
import no.nav.oebs.api.scim.repository.ScimGroupMembershipRepository;
import no.nav.oebs.api.scim.repository.ScimGroupRepository;
import no.nav.oebs.api.scim.repository.ScimUserRepository;
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integrasjonstester for health check-endepunkter.
 * Disse er @Unprotected og krever ikke JWT.
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
class HealthCheckIntegrationTest {

    @MockitoBean MultiIssuerConfiguration multiIssuerConfiguration;
    @MockitoBean KallLoggRepository kallLoggRepository;
    @MockitoBean KallLoggRepositoryImpl kallLoggRepositoryImpl;
    @MockitoBean PlsqlProcedureRepository plsqlProcedureRepository;
    @MockitoBean ScimUserRepository scimUserRepository;
    @MockitoBean ScimGroupRepository scimGroupRepository;
    @MockitoBean ScimGroupMembershipRepository scimGroupMembershipRepository;

    @LocalServerPort int port;

    private final RestTemplate restTemplate = new RestTemplate();

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    @Test
    void isAlive_returns200() {
        ResponseEntity<Void> response = restTemplate.getForEntity(url("/internal/isalive"), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void isReady_returns200() {
        ResponseEntity<Void> response = restTemplate.getForEntity(url("/internal/isready"), Void.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}



