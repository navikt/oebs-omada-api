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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integrasjonstest for lasting av applikasjonskonteksten.
 * Mocker alle JPA/DB- og JWT-avhengigheter slik at testen kjører uten infrastruktur.
 */
@SpringBootTest(
    classes = Application.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
        "spring.autoconfigure.exclude=" +
            "org.apache.directory.scim.spring.ScimpleSpringConfiguration," +
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration," +
            "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration," +
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
        "spring.jpa.open-in-view=false"
    }
)
@ActiveProfiles("test")
class ApplicationTest {

    // Mock MultiIssuerConfiguration to prevent eager OIDC metadata fetch at startup
    @MockitoBean MultiIssuerConfiguration multiIssuerConfiguration;

    @MockitoBean KallLoggRepository kallLoggRepository;
    @MockitoBean KallLoggRepositoryImpl kallLoggRepositoryImpl;
    @MockitoBean PlsqlProcedureRepository plsqlProcedureRepository;
    @MockitoBean ScimUserRepository scimUserRepository;
    @MockitoBean ScimGroupRepository scimGroupRepository;
    @MockitoBean ScimGroupMembershipRepository scimGroupMembershipRepository;


    @Test
    void applicationContextShouldLoad() {
    }
}