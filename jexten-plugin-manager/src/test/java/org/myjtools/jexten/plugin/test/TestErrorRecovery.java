package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.jexten.plugin.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for error recovery scenarios:
 * - Corrupted manifests
 * - Missing artifacts
 * - Invalid plugin files
 * - Installation failures
 */
@DisplayName("Error Recovery Tests")
class TestErrorRecovery {

    @TempDir
    Path tempDir;

    private PluginManager pluginManager;

    @BeforeEach
    void setUp() {
        pluginManager = new PluginManager("Test-Application", TestErrorRecovery.class.getClassLoader(), tempDir);
    }


    @Nested
    @DisplayName("Bundle Installation Errors")
    class BundleInstallationErrors {

        @Test
        @DisplayName("should throw exception when bundle file does not exist")
        void shouldThrowWhenBundleFileNotExists() {
            Path nonExistentFile = tempDir.resolve("non-existent-bundle.zip");

            assertThatThrownBy(() -> pluginManager.installPluginFromBundle(nonExistentFile))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("NoSuchFileException");
        }


        @Test
        @DisplayName("should throw exception when bundle file is empty")
        void shouldThrowWhenBundleFileEmpty() throws IOException {
            Path emptyFile = tempDir.resolve("empty-bundle.zip");
            Files.createFile(emptyFile);

            assertThatThrownBy(() -> pluginManager.installPluginFromBundle(emptyFile))
                .isInstanceOf(PluginException.class);
        }


        @Test
        @DisplayName("should throw exception when bundle is not a valid ZIP")
        void shouldThrowWhenBundleNotValidZip() throws IOException {
            Path invalidZip = tempDir.resolve("invalid-bundle.zip");
            Files.writeString(invalidZip, "This is not a valid ZIP file content");

            assertThatThrownBy(() -> pluginManager.installPluginFromBundle(invalidZip))
                .isInstanceOf(PluginException.class);
        }


        @Test
        @DisplayName("should throw exception when bundle ZIP has no manifest")
        void shouldThrowWhenBundleHasNoManifest() throws IOException {
            Path zipWithoutManifest = tempDir.resolve("no-manifest.zip");
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipWithoutManifest))) {
                ZipEntry entry = new ZipEntry("some-file.txt");
                zos.putNextEntry(entry);
                zos.write("dummy content".getBytes());
                zos.closeEntry();
            }

            assertThatThrownBy(() -> pluginManager.installPluginFromBundle(zipWithoutManifest))
                .isInstanceOf(PluginException.class);
        }


        @Test
        @DisplayName("should throw exception when bundle has corrupted manifest YAML")
        void shouldThrowWhenBundleHasCorruptedManifest() throws IOException {
            Path zipWithBadManifest = tempDir.resolve("bad-manifest.zip");
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipWithBadManifest))) {
                ZipEntry entry = new ZipEntry("plugin.yaml");
                zos.putNextEntry(entry);
                zos.write("this is not valid YAML: [[[{{{{".getBytes());
                zos.closeEntry();
            }

            assertThatThrownBy(() -> pluginManager.installPluginFromBundle(zipWithBadManifest))
                .isInstanceOf(PluginException.class);
        }
    }


    @Nested
    @DisplayName("JAR Installation Errors")
    class JarInstallationErrors {

        @Test
        @DisplayName("should throw exception when JAR file does not exist")
        void shouldThrowWhenJarFileNotExists() {
            Path nonExistentFile = tempDir.resolve("non-existent.jar");

            assertThatThrownBy(() -> pluginManager.installPluginFromJar(nonExistentFile))
                .isInstanceOf(PluginException.class);
        }


        @Test
        @DisplayName("should throw exception when artifact store is not set")
        void shouldThrowWhenArtifactStoreNotSet() throws IOException {
            // Create a valid JAR with manifest that has dependencies
            Path jarFile = createMinimalPluginJar("test-plugin-1.0.jar", """
                group: test.group
                name: test-plugin
                version: 1.0.0
                application: Test-Application
                hostModule: test.module
                artifacts:
                  org.example:
                    - some-dependency-1.0
                """);

            assertThatThrownBy(() -> pluginManager.installPluginFromJar(jarFile))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("Artifact store is not set");
        }
    }


    @Nested
    @DisplayName("Plugin Removal Errors")
    class PluginRemovalErrors {

        @Test
        @DisplayName("should throw exception when removing non-existent plugin")
        void shouldThrowWhenRemovingNonExistentPlugin() {
            PluginID nonExistentId = new PluginID("non.existent", "plugin");

            assertThatThrownBy(() -> pluginManager.removePlugin(nonExistentId))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("is not present");
        }
    }


    @Nested
    @DisplayName("Plugin Reload Errors")
    class PluginReloadErrors {

        @Test
        @DisplayName("should throw exception when reloading non-installed plugin")
        void shouldThrowWhenReloadingNonInstalledPlugin() {
            PluginID nonExistentId = new PluginID("non.existent", "plugin");

            assertThatThrownBy(() -> pluginManager.reloadPlugin(nonExistentId))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("is not installed");
        }
    }


    @Nested
    @DisplayName("Manifest Parsing Errors")
    class ManifestParsingErrors {

        @Test
        @DisplayName("should throw InvalidManifestException for completely invalid YAML")
        void shouldThrowForInvalidYaml() {
            String malformedYaml = "{{{{not valid yaml at all::::";

            assertThatThrownBy(() -> PluginManifest.read(new StringReader(malformedYaml)))
                .isInstanceOf(Exception.class);
        }


        @Test
        @DisplayName("should throw InvalidManifestException when required fields missing")
        void shouldThrowWhenRequiredFieldsMissing() {
            String incompleteManifest = """
                displayName: My Plugin
                description: A test plugin
                """;

            assertThatThrownBy(() -> PluginManifest.read(new StringReader(incompleteManifest)))
                .isInstanceOf(InvalidManifestException.class)
                .satisfies(ex -> {
                    var errors = ((InvalidManifestException) ex).validationErrors();
                    assertThat(errors).isNotEmpty();
                });
        }


        @Test
        @DisplayName("should throw InvalidManifestException for null input")
        void shouldThrowForNullInput() {
            assertThatThrownBy(() -> PluginManifest.read(null))
                .isInstanceOf(Exception.class);
        }
    }


    @Nested
    @DisplayName("Validation Errors")
    class ValidationErrors {

        @Test
        @DisplayName("should throw PluginValidationException when custom validator fails")
        void shouldThrowWhenCustomValidatorFails() throws IOException {
            // Add a validator that always fails
            pluginManager.addValidator(manifest ->
                ValidationResult.invalid("Custom validation failed: test error")
            );

            Path zipFile = createValidPluginBundle();

            assertThatThrownBy(() -> pluginManager.installPluginFromBundle(zipFile))
                .isInstanceOf(PluginValidationException.class)
                .satisfies(ex -> {
                    var errors = ((PluginValidationException) ex).validationErrors();
                    assertThat(errors).contains("Custom validation failed: test error");
                });
        }


        @Test
        @DisplayName("should collect errors from multiple validators")
        void shouldCollectErrorsFromMultipleValidators() throws IOException {
            pluginManager.addValidator(manifest ->
                ValidationResult.invalid("Error from validator 1")
            );
            pluginManager.addValidator(manifest ->
                ValidationResult.invalid("Error from validator 2")
            );

            Path zipFile = createValidPluginBundle();

            assertThatThrownBy(() -> pluginManager.installPluginFromBundle(zipFile))
                .isInstanceOf(PluginValidationException.class)
                .satisfies(ex -> {
                    var errors = ((PluginValidationException) ex).validationErrors();
                    assertThat(errors).hasSize(2);
                    assertThat(errors).contains("Error from validator 1", "Error from validator 2");
                });
        }
    }


    @Nested
    @DisplayName("Application Compatibility Errors")
    class ApplicationCompatibilityErrors {

        @Test
        @DisplayName("should reject plugin for different application")
        void shouldRejectPluginForDifferentApplication() throws IOException {
            Path zipFile = createPluginBundleForApp("Different-Application");

            // Should throw PluginException for incompatible application
            assertThatThrownBy(() -> pluginManager.installPluginFromBundle(zipFile))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("not compatible with this application");
        }
    }


    @Nested
    @DisplayName("Missing Artifacts Errors")
    class MissingArtifactsErrors {

        @Test
        @DisplayName("should throw when referenced artifact is missing during discovery")
        void shouldThrowWhenArtifactMissingDuringDiscovery() throws IOException {
            // Create manifest referencing non-existent artifact
            Path manifestDir = tempDir.resolve("manifests");
            Files.createDirectories(manifestDir);

            Path manifestFile = manifestDir.resolve("test-group-test-plugin.yaml");
            Files.writeString(manifestFile, """
                group: test-group
                name: test-plugin
                version: 1.0.0
                application: Test-Application
                hostModule: test.module
                artifacts:
                  test-group:
                    - missing-artifact-1.0
                """);

            // Create new plugin manager which will try to discover this plugin
            PluginManager pm = new PluginManager("Test-Application", getClass().getClassLoader(), tempDir);

            // Plugin should not be loaded due to missing artifact
            assertThat(pm.plugins()).isEmpty();
        }
    }


    @Nested
    @DisplayName("Checksum Verification Errors")
    class ChecksumVerificationErrors {

        @Test
        @DisplayName("should install plugin successfully when checksums are correct")
        void shouldInstallWhenChecksumsCorrect() throws IOException, NoSuchAlgorithmException {
            byte[] jarContent = createMinimalJarContent();
            String checksum = calculateSha256(jarContent);

            Path zipFile = createPluginBundleWithChecksum("test-plugin-1.0.0.jar", jarContent, checksum);

            assertThatCode(() -> pluginManager.installPluginFromBundle(zipFile))
                .doesNotThrowAnyException();

            assertThat(pluginManager.plugins()).hasSize(1);
        }


        @Test
        @DisplayName("should throw exception when checksum does not match")
        void shouldThrowWhenChecksumMismatch() throws IOException, NoSuchAlgorithmException {
            byte[] jarContent = createMinimalJarContent();
            String wrongChecksum = "0000000000000000000000000000000000000000000000000000000000000000";

            Path zipFile = createPluginBundleWithChecksum("test-plugin-1.0.0.jar", jarContent, wrongChecksum);

            assertThatThrownBy(() -> pluginManager.installPluginFromBundle(zipFile))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("Checksum verification failed")
                .hasMessageContaining("has been modified or corrupted");
        }


        @Test
        @DisplayName("should install plugin without checksums (backwards compatibility)")
        void shouldInstallWithoutChecksums() throws IOException {
            Path zipFile = createValidPluginBundleWithValidJar();

            assertThatCode(() -> pluginManager.installPluginFromBundle(zipFile))
                .doesNotThrowAnyException();

            assertThat(pluginManager.plugins()).hasSize(1);
        }


        @Test
        @DisplayName("should throw during discovery when installed artifact has wrong checksum")
        void shouldThrowDuringDiscoveryWhenChecksumMismatch() throws IOException {
            // First install a plugin without checksums
            Path zipFile = createValidPluginBundleWithValidJar();
            pluginManager.installPluginFromBundle(zipFile);
            assertThat(pluginManager.plugins()).hasSize(1);

            // Now modify the manifest to include a wrong checksum
            Path manifestFile = tempDir.resolve("manifests/test-group-test-plugin.yaml");
            String manifestContent = Files.readString(manifestFile);
            manifestContent += """
                checksums:
                  test-plugin-1.0.0.jar: 0000000000000000000000000000000000000000000000000000000000000000
                """;
            Files.writeString(manifestFile, manifestContent);

            // Create new plugin manager which will try to discover this plugin
            PluginManager pm = new PluginManager("Test-Application", getClass().getClassLoader(), tempDir);

            // Plugin should not be loaded due to checksum mismatch
            assertThat(pm.plugins()).isEmpty();
        }


        private byte[] createMinimalJarContent() throws IOException {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                // Add a minimal MANIFEST.MF
                ZipEntry manifestEntry = new ZipEntry("META-INF/MANIFEST.MF");
                zos.putNextEntry(manifestEntry);
                zos.write("Manifest-Version: 1.0\n".getBytes());
                zos.closeEntry();
            }
            return baos.toByteArray();
        }


        private Path createValidPluginBundleWithValidJar() throws IOException {
            Path zipFile = tempDir.resolve("test-bundle-valid-jar.zip");
            String manifestContent = """
                group: test-group
                name: test-plugin
                version: 1.0.0
                application: Test-Application
                hostModule: test.module
                displayName: Test Plugin
                description: A test plugin
                url: http://example.com
                licenseName: MIT
                licenseText: MIT License
                artifacts:
                  test-group:
                    - test-plugin-1.0.0
                """;

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                // Add plugin manifest
                ZipEntry manifestEntry = new ZipEntry("plugin.yaml");
                zos.putNextEntry(manifestEntry);
                zos.write(manifestContent.getBytes());
                zos.closeEntry();

                // Add a valid JAR (which is a ZIP with META-INF/MANIFEST.MF)
                ZipEntry jarEntry = new ZipEntry("test-plugin-1.0.0.jar");
                zos.putNextEntry(jarEntry);
                zos.write(createMinimalJarContent());
                zos.closeEntry();
            }
            return zipFile;
        }


        private Path createPluginBundleWithChecksum(String jarFileName, byte[] jarContent, String checksum)
                throws IOException {
            Path zipFile = tempDir.resolve("test-bundle-with-checksum.zip");
            String manifestContent = """
                group: test-group
                name: test-plugin
                version: 1.0.0
                application: Test-Application
                hostModule: test.module
                displayName: Test Plugin
                description: A test plugin
                url: http://example.com
                licenseName: MIT
                licenseText: MIT License
                artifacts:
                  test-group:
                    - test-plugin-1.0.0
                checksums:
                  %s: %s
                """.formatted(jarFileName, checksum);

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                // Add manifest
                ZipEntry manifestEntry = new ZipEntry("plugin.yaml");
                zos.putNextEntry(manifestEntry);
                zos.write(manifestContent.getBytes());
                zos.closeEntry();

                // Add the jar file
                ZipEntry jarEntry = new ZipEntry(jarFileName);
                zos.putNextEntry(jarEntry);
                zos.write(jarContent);
                zos.closeEntry();
            }
            return zipFile;
        }


        private String calculateSha256(byte[] content) throws NoSuchAlgorithmException {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(content);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        }
    }


    // Helper methods

    private Path createMinimalPluginJar(String fileName, String manifestContent) throws IOException {
        Path jarFile = tempDir.resolve(fileName);
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(jarFile))) {
            ZipEntry entry = new ZipEntry("plugin.yaml");
            zos.putNextEntry(entry);
            zos.write(manifestContent.getBytes());
            zos.closeEntry();
        }
        return jarFile;
    }


    private Path createValidPluginBundle() throws IOException {
        return createPluginBundleForApp("Test-Application");
    }


    private Path createPluginBundleForApp(String application) throws IOException {
        Path zipFile = tempDir.resolve("test-bundle.zip");
        String manifestContent = """
            group: test-group
            name: test-plugin
            version: 1.0.0
            application: %s
            hostModule: test.module
            displayName: Test Plugin
            description: A test plugin
            url: http://example.com
            licenseName: MIT
            licenseText: MIT License
            artifacts:
              test-group:
                - test-plugin-1.0.0
            """.formatted(application);

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipFile))) {
            // Add manifest
            ZipEntry manifestEntry = new ZipEntry("plugin.yaml");
            zos.putNextEntry(manifestEntry);
            zos.write(manifestContent.getBytes());
            zos.closeEntry();

            // Add a dummy jar file
            ZipEntry jarEntry = new ZipEntry("test-plugin-1.0.0.jar");
            zos.putNextEntry(jarEntry);
            zos.write("dummy jar content".getBytes());
            zos.closeEntry();
        }
        return zipFile;
    }
}
