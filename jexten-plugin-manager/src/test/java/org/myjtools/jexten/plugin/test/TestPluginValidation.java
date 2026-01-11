package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.myjtools.jexten.plugin.PluginManifest;
import org.myjtools.jexten.plugin.PluginValidator;
import org.myjtools.jexten.plugin.ValidationResult;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


public class TestPluginValidation {


    @Nested
    class ValidationResultTests {

        @Test
        void validResultShouldBeValid() {
            ValidationResult result = ValidationResult.valid();
            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        void invalidResultWithSingleError() {
            ValidationResult result = ValidationResult.invalid("Error message");
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).containsExactly("Error message");
        }

        @Test
        void invalidResultWithMultipleErrors() {
            ValidationResult result = ValidationResult.invalid(List.of("Error 1", "Error 2", "Error 3"));
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).containsExactly("Error 1", "Error 2", "Error 3");
        }

        @Test
        void emptyErrorListShouldBeValid() {
            ValidationResult result = ValidationResult.invalid(List.of());
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void mergeValidWithValidShouldBeValid() {
            ValidationResult result = ValidationResult.valid().merge(ValidationResult.valid());
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void mergeValidWithInvalidShouldBeInvalid() {
            ValidationResult result = ValidationResult.valid()
                    .merge(ValidationResult.invalid("Error"));
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).containsExactly("Error");
        }

        @Test
        void mergeInvalidWithValidShouldBeInvalid() {
            ValidationResult result = ValidationResult.invalid("Error")
                    .merge(ValidationResult.valid());
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).containsExactly("Error");
        }

        @Test
        void mergeInvalidWithInvalidShouldCombineErrors() {
            ValidationResult result = ValidationResult.invalid("Error 1")
                    .merge(ValidationResult.invalid("Error 2"));
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).containsExactly("Error 1", "Error 2");
        }

        @Test
        void toStringShouldDescribeResult() {
            assertThat(ValidationResult.valid().toString()).contains("valid");
            assertThat(ValidationResult.invalid("Error").toString()).contains("invalid");
            assertThat(ValidationResult.invalid("Error").toString()).contains("Error");
        }
    }


    @Nested
    class PluginValidatorTests {

        private PluginManifest createManifest(String yaml) {
            return PluginManifest.read(new StringReader(yaml));
        }

        private final String baseManifest = """
            group: com.example
            name: test-plugin
            version: 1.0.0
            hostModule: com.example.test
            """;

        @Test
        void requireLicenseShouldRejectMissingLicense() {
            PluginManifest manifest = createManifest(baseManifest);
            ValidationResult result = PluginValidator.requireLicense().validate(manifest);
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).containsIgnoringCase("license");
        }

        @Test
        void requireLicenseShouldAcceptPresentLicense() {
            PluginManifest manifest = createManifest(baseManifest + "licenseName: MIT\n");
            ValidationResult result = PluginValidator.requireLicense().validate(manifest);
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void requireUrlShouldRejectMissingUrl() {
            PluginManifest manifest = createManifest(baseManifest);
            ValidationResult result = PluginValidator.requireUrl().validate(manifest);
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).containsIgnoringCase("url");
        }

        @Test
        void requireUrlShouldAcceptPresentUrl() {
            PluginManifest manifest = createManifest(baseManifest + "url: https://example.com\n");
            ValidationResult result = PluginValidator.requireUrl().validate(manifest);
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void requireDescriptionShouldRejectMissingDescription() {
            PluginManifest manifest = createManifest(baseManifest);
            ValidationResult result = PluginValidator.requireDescription().validate(manifest);
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).containsIgnoringCase("description");
        }

        @Test
        void requireDescriptionShouldAcceptPresentDescription() {
            PluginManifest manifest = createManifest(baseManifest + "description: A test plugin\n");
            ValidationResult result = PluginValidator.requireDescription().validate(manifest);
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void requireDisplayNameShouldRejectMissingDisplayName() {
            PluginManifest manifest = createManifest(baseManifest);
            ValidationResult result = PluginValidator.requireDisplayName().validate(manifest);
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).containsIgnoringCase("display name");
        }

        @Test
        void requireDisplayNameShouldAcceptPresentDisplayName() {
            PluginManifest manifest = createManifest(baseManifest + "displayName: Test Plugin\n");
            ValidationResult result = PluginValidator.requireDisplayName().validate(manifest);
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void requireMinimumVersionShouldRejectLowerVersion() {
            PluginManifest manifest = createManifest(baseManifest);  // version: 1.0.0
            ValidationResult result = PluginValidator.requireMinimumVersion("2.0.0").validate(manifest);
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors().get(0)).contains("below minimum");
        }

        @Test
        void requireMinimumVersionShouldAcceptEqualVersion() {
            PluginManifest manifest = createManifest(baseManifest);  // version: 1.0.0
            ValidationResult result = PluginValidator.requireMinimumVersion("1.0.0").validate(manifest);
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void requireMinimumVersionShouldAcceptHigherVersion() {
            String yaml = baseManifest.replace("version: 1.0.0", "version: 2.0.0");
            PluginManifest manifest = createManifest(yaml);
            ValidationResult result = PluginValidator.requireMinimumVersion("1.0.0").validate(manifest);
            assertThat(result.isValid()).isTrue();
        }

        @Test
        void andShouldCombineValidators() {
            PluginManifest manifest = createManifest(baseManifest);  // No license, no URL
            PluginValidator combined = PluginValidator.requireLicense()
                    .and(PluginValidator.requireUrl());
            ValidationResult result = combined.validate(manifest);
            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).hasSize(2);
        }

        @Test
        void customValidatorShouldWork() {
            PluginValidator customValidator = manifest -> {
                if (!manifest.group().startsWith("com.")) {
                    return ValidationResult.invalid("Group must start with 'com.'");
                }
                return ValidationResult.valid();
            };

            PluginManifest validManifest = createManifest(baseManifest);
            assertThat(customValidator.validate(validManifest).isValid()).isTrue();

            String invalidYaml = baseManifest.replace("group: com.example", "group: org.example");
            PluginManifest invalidManifest = createManifest(invalidYaml);
            assertThat(customValidator.validate(invalidManifest).isValid()).isFalse();
        }
    }
}
