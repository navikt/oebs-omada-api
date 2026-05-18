package no.nav.oebs.api.ping.v1;

import no.nav.oebs.api.health.HealthCheckDbProbe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PingControllerTest {

    @Mock
    private HealthCheckDbProbe healthCheckDbProbe;

    @Test
    void ping_callsDatabaseProbe() {
        PingController controller = new PingController(healthCheckDbProbe);

        controller.ping();

        verify(healthCheckDbProbe).pingDatabase();
    }
}

