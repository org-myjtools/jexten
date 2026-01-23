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


    // ========== compareTo() Tests ==========

    @Test
    void compareToShouldReturnZeroForEqualVersions() {
        Version v1 = Version.of("1.2.3");
        Version v2 = Version.of("1.2.3");
        assertThat(v1.compareTo(v2)).isEqualTo(0);
    }


    @Test
    void compareToShouldCompareByMajorFirst() {
        Version v1 = Version.of("1.9.9");
        Version v2 = Version.of("2.0.0");
        assertThat(v1.compareTo(v2)).isLessThan(0);
        assertThat(v2.compareTo(v1)).isGreaterThan(0);
    }


    @Test
    void compareToShouldCompareByMinorWhenMajorEquals() {
        Version v1 = Version.of("2.1.9");
        Version v2 = Version.of("2.3.0");
        assertThat(v1.compareTo(v2)).isLessThan(0);
        assertThat(v2.compareTo(v1)).isGreaterThan(0);
    }


    @Test
    void compareToShouldCompareByPatchWhenMajorAndMinorEqual() {
        Version v1 = Version.of("2.3.alpha");
        Version v2 = Version.of("2.3.beta");
        assertThat(v1.compareTo(v2)).isLessThan(0);
        assertThat(v2.compareTo(v1)).isGreaterThan(0);
    }


    @Test
    void compareToShouldHandleVersionsWithoutPatch() {
        Version v1 = Version.of("1.0");
        Version v2 = Version.of("1.1");
        assertThat(v1.compareTo(v2)).isLessThan(0);
    }


    @Test
    void compareToShouldHandleVersionsWithOnlyMajor() {
        Version v1 = Version.of("1");
        Version v2 = Version.of("2");
        assertThat(v1.compareTo(v2)).isLessThan(0);
    }


    // ========== toString() Tests ==========

    @Test
    void toStringShouldFormatMajorMinorPatch() {
        Version v = Version.of("1.2.3");
        assertThat(v.toString()).isEqualTo("1.2.3");
    }


    @Test
    void toStringShouldFormatMajorMinorOnly() {
        Version v = Version.of("1.2");
        assertThat(v.toString()).isEqualTo("1.2");
    }


    @Test
    void toStringShouldFormatMajorOnlyWithMinorZero() {
        Version v = Version.of("5");
        assertThat(v.toString()).isEqualTo("5.0");
    }


    @Test
    void toStringShouldIncludeTextPatch() {
        Version v = Version.of("1.0.SNAPSHOT");
        assertThat(v.toString()).isEqualTo("1.0.SNAPSHOT");
    }


    @Test
    void toStringShouldBeConsistentWithOf() {
        String original = "3.14.159";
        Version v = Version.of(original);
        assertThat(v.toString()).isEqualTo(original);
    }


    // ========== validate() Tests ==========

    @Test
    void validateShouldReturnTrueForValidVersions() {
        assertThat(Version.validate("1")).isTrue();
        assertThat(Version.validate("1.0")).isTrue();
        assertThat(Version.validate("1.0.0")).isTrue();
        assertThat(Version.validate("1.2.SNAPSHOT")).isTrue();
        assertThat(Version.validate("12.345.678")).isTrue();
    }


    @Test
    void validateShouldReturnFalseForInvalidVersions() {
        assertThat(Version.validate("")).isFalse();
        assertThat(Version.validate("   ")).isFalse();
        assertThat(Version.validate("abc")).isFalse();
        assertThat(Version.validate("a.b.c")).isFalse();
        assertThat(Version.validate("1.abc.0")).isFalse();
    }


    @Test
    void validateShouldReturnFalseForNull() {
        assertThat(Version.validate(null)).isFalse();
    }

}
