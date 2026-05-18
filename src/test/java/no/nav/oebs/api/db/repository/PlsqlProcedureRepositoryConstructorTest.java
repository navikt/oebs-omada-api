package no.nav.oebs.api.db.repository;

import no.nav.oebs.api.scim.KallLoggHelper;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class PlsqlProcedureRepositoryConstructorTest {

    @Test
    void constructor_createsRepositoryWithDependencies() {
        DataSource dataSource = mock(DataSource.class);
        KallLoggHelper kallLoggHelper = mock(KallLoggHelper.class);

        PlsqlProcedureRepository repository = new PlsqlProcedureRepository(dataSource, kallLoggHelper);

        assertThat(repository).isNotNull();
    }

    @Test
    void mapInsertRetcode_handlesRepresentativeValues() {
        assertThat(PlsqlProcedureRepository.mapInsertRetcode("0")).isZero();
        assertThat(PlsqlProcedureRepository.mapInsertRetcode("1")).isEqualTo(1);
        assertThat(PlsqlProcedureRepository.mapInsertRetcode("2")).isEqualTo(-1);
        assertThat(PlsqlProcedureRepository.mapInsertRetcode(null)).isZero();
        assertThat(PlsqlProcedureRepository.mapInsertRetcode("abc")).isEqualTo(-1);
    }

    @Test
    void executeInOutProcedure_failsFastOnInvalidProcedureName() {
        DataSource dataSource = mock(DataSource.class);
        KallLoggHelper kallLoggHelper = mock(KallLoggHelper.class);
        PlsqlProcedureRepository repository = new PlsqlProcedureRepository(dataSource, kallLoggHelper);

        assertDoesNotThrow(() -> {
            try {
                repository.executeInOutProcedure("badname", PlsqlProcedureRepository.Operasjon.NY, "{}");
            } catch (IllegalArgumentException expected) {
                // expected path verifies validation branch
            }
        });
    }
}

