package no.nav.oebs.api.config.common.logging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PropertySourceLoggerTest {

    private MockEnvironment environment;
    private PropertySourceLogger propertySourceLogger;

    @BeforeEach
    void setUp() {
        environment = new MockEnvironment();
        propertySourceLogger = new PropertySourceLogger(environment);
    }

    @Test
    void log_handlesMatchingPropertySource() {
        Properties properties = new Properties();
        properties.setProperty("username", "alice");
        properties.setProperty("db.password", "super-secret");
        environment.getPropertySources().addFirst(new PropertiesPropertySource("vault-main", properties));

        assertDoesNotThrow(() -> propertySourceLogger.log("vault"));
    }

    @Test
    void log_handlesMissingPropertySource() {
        assertDoesNotThrow(() -> propertySourceLogger.log("does-not-exist"));
    }

    @Test
    void maskIfPassword_masksKnownSecretKeys() {
        String masked = ReflectionTestUtils.invokeMethod(propertySourceLogger,
                "maskIfPassword", "db.password", "super-secret");

        assertThat(masked).isEqualTo("********");
    }

    @Test
    void maskIfPassword_keepsNonSecretValues() {
        String value = ReflectionTestUtils.invokeMethod(propertySourceLogger,
                "maskIfPassword", "username", "alice");

        assertThat(value).isEqualTo("alice");
    }
}

