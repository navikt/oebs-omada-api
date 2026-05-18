package no.nav.oebs.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StartupLoggerTest {

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
}

