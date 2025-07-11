package org.myjtools.jexten.test;

import static org.assertj.core.api.Assertions.*;

import org.myjtools.jexten.Version;
import org.junit.jupiter.api.Test;

class TestVersion {

    @Test
    void versionCanHaveOnlyMajorPart() {
        assertThat(Version.of("1")).isNotNull();
    }


    @Test
    void versionCanHaveOnlyMajorAndMinorParts() {
        assertThat(Version.of("1.2")).isNotNull();
    }


    @Test
    void versionCanHaveMajorMinorAndPatchParts() {
        assertThat(Version.of("1.2.3")).isNotNull();
    }


    @Test
    void versionCanHaveTextPacthPart() {
        assertThat(Version.of("1.2.cat")).isNotNull();
    }


    @Test
    void versionCannotHaveTextMajorPart() {
        assertThatCode(()-> Version.of("whale.1.1"))
            .isExactlyInstanceOf(IllegalArgumentException.class);
    }


    @Test
    void versionCannotHaveTextMinorPart() {
        assertThatCode(()-> Version.of("1.dog.1"))
            .isExactlyInstanceOf(IllegalArgumentException.class);
    }


    @Test
    void versionsAreNotCompatibleIfMajorPartDiffers() {
        Version v1_5 =  Version.of("1.5");
        Version v2_1 =  Version.of("2.1.patch");
        assertThat(v1_5.isCompatibleWith(v2_1)).isFalse();
        assertThat(v2_1.isCompatibleWith(v1_5)).isFalse();
    }


    @Test
    void versionsWithSameMajorPartAreOnlyCompatibleBackwards() {
        Version v2_0 =  Version.of("2.0");
        Version v2_1 =  Version.of("2.1.patch");
        assertThat(v2_1.isCompatibleWith(v2_0)).isTrue();
        assertThat(v2_0.isCompatibleWith(v2_1)).isFalse();
    }

}
