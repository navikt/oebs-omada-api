package no.nav.oebs.api.config.common.logging;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingUtilsTest {

    @Test
    void formatExceptionAsString_returnsNull_whenExceptionIsNull() {
        assertThat(LoggingUtils.formatExceptionAsString(null)).isNull();
    }

    @Test
    void formatExceptionAsString_containsMessageAndType() {
        String formatted = LoggingUtils.formatExceptionAsString(new IllegalStateException("boom"));

        assertThat(formatted)
                .contains("IllegalStateException")
                .contains("boom");
    }

    @Test
    void maskIfFnr_returnsNullLiteral_whenInputIsNull() {
        assertThat(LoggingUtils.maskIfFnr(null)).isEqualTo("(null)");
    }

    @Test
    void maskIfFnr_masksStandaloneFnr() {
        String masked = LoggingUtils.maskIfFnr("id=01020312345");

        assertThat(masked).isEqualTo("id=01*******45");
    }

    @Test
    void maskIfFnr_masksFnr_withNonDigitBoundaries() {
        String masked = LoggingUtils.maskIfFnr("x01020312345y");

        assertThat(masked).isEqualTo("x01*******45y");
    }

    @Test
    void maskIfFnr_doesNotMask_whenDigitsArePartOfLongerSequence() {
        String input = "0010203123459";

        assertThat(LoggingUtils.maskIfFnr(input)).isEqualTo(input);
    }

    @Test
    void maskIfFnr_doesNotMask_whenCandidateContainsNonDigitInside() {
        String input = "A12345X78901B";

        assertThat(LoggingUtils.maskIfFnr(input)).isEqualTo(input);
    }
}

