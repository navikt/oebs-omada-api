package no.nav.oebs.api;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class ApplicationMainTest {

    @Test
    void main_invokesSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            Application.main(new String[0]);
            mocked.verify(() -> SpringApplication.run(Application.class));
        }
    }
}


