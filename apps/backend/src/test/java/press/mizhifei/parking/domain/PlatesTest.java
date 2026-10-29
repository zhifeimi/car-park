package press.mizhifei.parking.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Same samples as normalizePlate in packages/utils/src/validation.test.ts. */
class PlatesTest {

    @Test
    void separatorsAndCase_collapseToOneSpelling() {
        for (String typed : new String[] {"AB-123", "ab 123", "AB·123", "AB123", "a b-1·23"}) {
            assertThat(Plates.canonical(typed)).as(typed).isEqualTo("AB123");
        }
    }

    @Test
    void mainlandPlates_areUnchanged() {
        assertThat(Plates.canonical("粤A12345")).isEqualTo("粤A12345");
        assertThat(Plates.canonical("京AD12345")).isEqualTo("京AD12345");
    }

    @Test
    void decomposedInput_isComposed() {
        // "É" typed as E + combining acute → the precomposed form
        assertThat(Plates.canonical("É-12")).isEqualTo("É12");
    }

    @Test
    void null_staysNull() {
        assertThat(Plates.canonical(null)).isNull();
    }
}
