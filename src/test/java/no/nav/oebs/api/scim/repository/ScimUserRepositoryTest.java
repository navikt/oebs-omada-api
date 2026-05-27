package no.nav.oebs.api.scim.repository;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class ScimUserRepositoryTest {

    @Test
    void findAllActiveUsers_queryIncludesPermisjonUsers() throws NoSuchMethodException {
        Method method = ScimUserRepository.class.getMethod("findAllActiveUsers", org.springframework.data.domain.Pageable.class);
        Query query = method.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("u.activeFlag = 'Y'")
                .contains("UPPER(COALESCE(u.permisjon, '')) = 'PERMISJON'");
    }

    @Test
    void countActiveUsers_queryIncludesPermisjonUsers() throws NoSuchMethodException {
        Method method = ScimUserRepository.class.getMethod("countActiveUsers");
        Query query = method.getAnnotation(Query.class);

        assertThat(query).isNotNull();
        assertThat(query.value())
                .contains("u.activeFlag = 'Y'")
                .contains("UPPER(COALESCE(u.permisjon, '')) = 'PERMISJON'");
    }
}

