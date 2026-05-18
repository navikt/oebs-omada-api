package no.nav.oebs.api;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApplicationMainTest {

    @Test
    void main_invokesSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            Application.main(new String[0]);
            mocked.verify(() -> SpringApplication.run(Application.class));
        }
    }

    @Test
    void runApplication_logsStartupDetails_whenInfoEnabled() {
        Logger logger = mock(Logger.class);
        when(logger.isInfoEnabled()).thenReturn(true);

        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            Application.runApplication(new String[0], logger);

            mocked.verify(() -> SpringApplication.run(Application.class));
            verify(logger).info("Starter oebs-omada-api...");
            verify(logger).info(eq("SpringApplication.run() fullført på {}ms"), anyLong());
        }
    }

    @Test
    void runApplication_skipsStartupLogs_whenInfoDisabled() {
        Logger logger = mock(Logger.class);
        when(logger.isInfoEnabled()).thenReturn(false);

        try (MockedStatic<SpringApplication> mocked = mockStatic(SpringApplication.class)) {
            Application.runApplication(new String[0], logger);

            mocked.verify(() -> SpringApplication.run(Application.class));
            verify(logger, never()).info("Starter oebs-omada-api...");
            verify(logger, never()).info(eq("SpringApplication.run() fullført på {}ms"), anyLong());
        }
    }
}


