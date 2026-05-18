package no.nav.oebs.api.config;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.management.ManagementFactory;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

class StartupLoggerTest {

    @Test
    void onContextRefreshed_returnsEarly_forChildContext() {
        ApplicationContext appContext = mock(ApplicationContext.class);
        ApplicationContext parentContext = mock(ApplicationContext.class);
        ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);
        when(event.getApplicationContext()).thenReturn(appContext);
        when(appContext.getParent()).thenReturn(parentContext);

        StartupLogger logger = new StartupLogger(appContext, new MockEnvironment());

        assertDoesNotThrow(() -> logger.onContextRefreshed(event));
    }

    @Test
    void onContextRefreshed_logsProfiles_forRootContext() {
        ApplicationContext appContext = mock(ApplicationContext.class);
        ContextRefreshedEvent event = mock(ContextRefreshedEvent.class);
        when(event.getApplicationContext()).thenReturn(appContext);
        when(appContext.getParent()).thenReturn(null);

        ConfigurableEnvironment environment = new MockEnvironment();
        StartupLogger logger = new StartupLogger(appContext, environment);

        assertDoesNotThrow(() -> logger.onContextRefreshed(event));
    }

    @Test
    void logScimStatus_doesNotThrow_whenBeansMissing() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.containsBean("scimpleServlet")).thenReturn(false);
        when(applicationContext.containsBean("repositoryRegistry")).thenReturn(false);
        when(applicationContext.containsBean("schemaRegistry")).thenReturn(false);
        when(applicationContext.containsBean("dataSource")).thenReturn(false);
        ConfigurableEnvironment environment = new MockEnvironment();

        StartupLogger logger = new StartupLogger(applicationContext, environment);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(logger, "logScimStatus"));
    }

    @Test
    void logScimStatus_doesNotThrow_whenBeansPresent() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        when(applicationContext.containsBean("scimpleServlet")).thenReturn(true);
        when(applicationContext.containsBean("repositoryRegistry")).thenReturn(true);
        when(applicationContext.containsBean("schemaRegistry")).thenReturn(true);
        ConfigurableEnvironment environment = new MockEnvironment();

        StartupLogger logger = new StartupLogger(applicationContext, environment);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(logger, "logScimStatus"));
    }

    @Test
    void logSummary_doesNotThrow() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        ConfigurableEnvironment environment = new MockEnvironment().withProperty("server.port", "8080");
        StartupLogger logger = new StartupLogger(applicationContext, environment);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(logger, "logSummary"));
    }

    @Test
    void logServletMappings_handlesRuntimeException() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        ConfigurableEnvironment environment = new MockEnvironment();
        StartupLogger logger = new StartupLogger(applicationContext, environment);
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);
        when(event.getApplicationContext()).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(logger, "logServletMappings", event));
    }

    @Test
    void logSummary_handlesRuntimeExceptionFromManagementFactory() {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        ConfigurableEnvironment environment = new MockEnvironment().withProperty("server.port", "8080");
        StartupLogger logger = new StartupLogger(applicationContext, environment);

        try (MockedStatic<ManagementFactory> mocked = mockStatic(ManagementFactory.class)) {
            mocked.when(ManagementFactory::getRuntimeMXBean).thenThrow(new RuntimeException("mx fail"));
            assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(logger, "logSummary"));
        }
    }
}

