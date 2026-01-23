package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.jexten.plugin.PluginException;
import org.myjtools.jexten.plugin.PluginManifest;
import org.myjtools.jexten.plugin.PluginValidator;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;

import static org.assertj.core.api.Assertions.*;


@DisplayName("Plugin Checksum Verification Tests")
public class TestPluginChecksumVerification {

    @TempDir
    Path tempDir;


    @Nested
    @DisplayName("calculateSha256 Tests")
    class CalculateSha256Tests {

        @Test
        @DisplayName("should calculate correct SHA-256 for known content")
        void shouldCalculateCorrectSha256ForKnownContent() throws IOException, NoSuchAlgorithmException {
            // Create a file with known content
            Path testFile = tempDir.resolve("test.txt");
            Files.writeString(testFile, "Hello, World!");

            String hash = PluginValidator.calculateSha256(testFile);

            // Known SHA-256 hash for "Hello, World!"
            assertThat(hash).isEqualTo("dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f");
        }

        @Test
        @DisplayName("should calculate correct SHA-256 for empty file")
        void shouldCalculateCorrectSha256ForEmptyFile() throws IOException, NoSuchAlgorithmException {
            Path testFile = tempDir.resolve("empty.txt");
            Files.writeString(testFile, "");

            String hash = PluginValidator.calculateSha256(testFile);

            // Known SHA-256 hash for empty content
            assertThat(hash).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        }

        @Test
        @DisplayName("should produce lowercase hex string")
        void shouldProduceLowercaseHexString() throws IOException, NoSuchAlgorithmException {
            Path testFile = tempDir.resolve("test.txt");
            Files.writeString(testFile, "test content");

            String hash = PluginValidator.calculateSha256(testFile);

            assertThat(hash).matches("[0-9a-f]{64}");
            assertThat(hash).isEqualTo(hash.toLowerCase());
        }

        @Test
        @DisplayName("should handle binary content correctly")
        void shouldHandleBinaryContent() throws IOException, NoSuchAlgorithmException {
            Path testFile = tempDir.resolve("binary.bin");
            byte[] binaryContent = new byte[256];
            for (int i = 0; i < 256; i++) {
                binaryContent[i] = (byte) i;
            }
            Files.write(testFile, binaryContent);

            String hash = PluginValidator.calculateSha256(testFile);

            assertThat(hash).hasSize(64);
            assertThat(hash).matches("[0-9a-f]+");
        }

        @Test
        @DisplayName("should handle large files")
        void shouldHandleLargeFiles() throws IOException, NoSuchAlgorithmException {
            Path testFile = tempDir.resolve("large.bin");
            // Create a 1MB file
            byte[] content = new byte[1024 * 1024];
            for (int i = 0; i < content.length; i++) {
                content[i] = (byte) (i % 256);
            }
            Files.write(testFile, content);

            String hash = PluginValidator.calculateSha256(testFile);

            assertThat(hash).hasSize(64);
        }

        @Test
        @DisplayName("should produce consistent hash for same content")
        void shouldProduceConsistentHash() throws IOException, NoSuchAlgorithmException {
            Path file1 = tempDir.resolve("file1.txt");
            Path file2 = tempDir.resolve("file2.txt");
            String content = "Identical content for both files";
            Files.writeString(file1, content);
            Files.writeString(file2, content);

            String hash1 = PluginValidator.calculateSha256(file1);
            String hash2 = PluginValidator.calculateSha256(file2);

            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("should produce different hash for different content")
        void shouldProduceDifferentHashForDifferentContent() throws IOException, NoSuchAlgorithmException {
            Path file1 = tempDir.resolve("file1.txt");
            Path file2 = tempDir.resolve("file2.txt");
            Files.writeString(file1, "Content A");
            Files.writeString(file2, "Content B");

            String hash1 = PluginValidator.calculateSha256(file1);
            String hash2 = PluginValidator.calculateSha256(file2);

            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("should throw IOException for non-existent file")
        void shouldThrowIOExceptionForNonExistentFile() {
            Path nonExistent = tempDir.resolve("nonexistent.txt");

            assertThatThrownBy(() -> PluginValidator.calculateSha256(nonExistent))
                .isInstanceOf(IOException.class);
        }
    }


    @Nested
    @DisplayName("verifyChecksum Tests")
    class VerifyChecksumTests {

        private PluginManifest createTestManifest() {
            return PluginManifest.read(new StringReader("""
                group: com.example
                name: test-plugin
                version: 1.0.0
                hostModule: com.example.test
                """));
        }

        @Test
        @DisplayName("should pass verification with correct checksum")
        void shouldPassWithCorrectChecksum() throws IOException, NoSuchAlgorithmException {
            Path artifactFile = tempDir.resolve("artifact.jar");
            Files.writeString(artifactFile, "artifact content");

            String correctChecksum = PluginValidator.calculateSha256(artifactFile);
            PluginManifest manifest = createTestManifest();

            // Should not throw
            assertThatCode(() -> PluginValidator.verifyChecksum(manifest, artifactFile, correctChecksum))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should pass verification with uppercase checksum")
        void shouldPassWithUppercaseChecksum() throws IOException, NoSuchAlgorithmException {
            Path artifactFile = tempDir.resolve("artifact.jar");
            Files.writeString(artifactFile, "artifact content");

            String correctChecksum = PluginValidator.calculateSha256(artifactFile).toUpperCase();
            PluginManifest manifest = createTestManifest();

            // Should not throw (equalsIgnoreCase is used)
            assertThatCode(() -> PluginValidator.verifyChecksum(manifest, artifactFile, correctChecksum))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should fail verification with incorrect checksum")
        void shouldFailWithIncorrectChecksum() throws IOException {
            Path artifactFile = tempDir.resolve("artifact.jar");
            Files.writeString(artifactFile, "artifact content");

            String incorrectChecksum = "0000000000000000000000000000000000000000000000000000000000000000";
            PluginManifest manifest = createTestManifest();

            assertThatThrownBy(() -> PluginValidator.verifyChecksum(manifest, artifactFile, incorrectChecksum))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("Checksum verification failed")
                .hasMessageContaining("modified or corrupted");
        }

        @Test
        @DisplayName("should include plugin ID in error message")
        void shouldIncludePluginIdInErrorMessage() throws IOException {
            Path artifactFile = tempDir.resolve("artifact.jar");
            Files.writeString(artifactFile, "content");

            String incorrectChecksum = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
            PluginManifest manifest = createTestManifest();

            assertThatThrownBy(() -> PluginValidator.verifyChecksum(manifest, artifactFile, incorrectChecksum))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining(manifest.id().toString());
        }

        @Test
        @DisplayName("should include artifact filename in error message")
        void shouldIncludeArtifactFilenameInErrorMessage() throws IOException {
            Path artifactFile = tempDir.resolve("my-artifact-1.0.0.jar");
            Files.writeString(artifactFile, "content");

            String incorrectChecksum = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
            PluginManifest manifest = createTestManifest();

            assertThatThrownBy(() -> PluginValidator.verifyChecksum(manifest, artifactFile, incorrectChecksum))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("my-artifact-1.0.0.jar");
        }

        @Test
        @DisplayName("should include expected and actual checksums in error message")
        void shouldIncludeExpectedAndActualChecksumsInErrorMessage() throws IOException, NoSuchAlgorithmException {
            Path artifactFile = tempDir.resolve("artifact.jar");
            Files.writeString(artifactFile, "content");

            String actualChecksum = PluginValidator.calculateSha256(artifactFile);
            String expectedChecksum = "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
            PluginManifest manifest = createTestManifest();

            assertThatThrownBy(() -> PluginValidator.verifyChecksum(manifest, artifactFile, expectedChecksum))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining(expectedChecksum)
                .hasMessageContaining(actualChecksum);
        }

        @Test
        @DisplayName("should throw PluginException when file does not exist")
        void shouldThrowPluginExceptionWhenFileDoesNotExist() {
            Path nonExistent = tempDir.resolve("nonexistent.jar");
            PluginManifest manifest = createTestManifest();
            String anyChecksum = "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd";

            assertThatThrownBy(() -> PluginValidator.verifyChecksum(manifest, nonExistent, anyChecksum))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("Cannot verify checksum");
        }

        @Test
        @DisplayName("should detect tampering after file modification")
        void shouldDetectTamperingAfterFileModification() throws IOException, NoSuchAlgorithmException {
            Path artifactFile = tempDir.resolve("artifact.jar");
            Files.writeString(artifactFile, "original content");

            // Get original checksum
            String originalChecksum = PluginValidator.calculateSha256(artifactFile);

            // Tamper with the file
            Files.writeString(artifactFile, "modified content");

            PluginManifest manifest = createTestManifest();

            // Verification should fail
            assertThatThrownBy(() -> PluginValidator.verifyChecksum(manifest, artifactFile, originalChecksum))
                .isInstanceOf(PluginException.class)
                .hasMessageContaining("Checksum verification failed");
        }
    }


    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("should verify checksum workflow: create, hash, verify")
        void shouldVerifyChecksumWorkflow() throws IOException, NoSuchAlgorithmException {
            // Simulate plugin installation workflow
            Path pluginJar = tempDir.resolve("plugin-1.0.0.jar");
            byte[] jarContent = "PK\u0003\u0004...mock jar content...".getBytes(StandardCharsets.UTF_8);
            Files.write(pluginJar, jarContent);

            // Calculate checksum (as would be done when building the plugin)
            String checksum = PluginValidator.calculateSha256(pluginJar);

            // Create manifest with checksum
            PluginManifest manifest = PluginManifest.read(new StringReader("""
                group: com.example
                name: my-plugin
                version: 1.0.0
                hostModule: com.example.plugin
                checksums:
                  plugin-1.0.0.jar: %s
                """.formatted(checksum)));

            // Verify checksum (as would be done when loading the plugin)
            String storedChecksum = manifest.checksums().get("plugin-1.0.0.jar");

            assertThatCode(() -> PluginValidator.verifyChecksum(manifest, pluginJar, storedChecksum))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should verify multiple artifacts in sequence")
        void shouldVerifyMultipleArtifactsInSequence() throws IOException, NoSuchAlgorithmException {
            // Create multiple artifact files
            Path artifact1 = tempDir.resolve("lib-a-1.0.jar");
            Path artifact2 = tempDir.resolve("lib-b-2.0.jar");
            Path artifact3 = tempDir.resolve("lib-c-3.0.jar");

            Files.writeString(artifact1, "Library A content");
            Files.writeString(artifact2, "Library B content");
            Files.writeString(artifact3, "Library C content");

            String checksum1 = PluginValidator.calculateSha256(artifact1);
            String checksum2 = PluginValidator.calculateSha256(artifact2);
            String checksum3 = PluginValidator.calculateSha256(artifact3);

            PluginManifest manifest = PluginManifest.read(new StringReader("""
                group: com.example
                name: multi-lib-plugin
                version: 1.0.0
                hostModule: com.example.plugin
                """));

            // All verifications should pass
            assertThatCode(() -> {
                PluginValidator.verifyChecksum(manifest, artifact1, checksum1);
                PluginValidator.verifyChecksum(manifest, artifact2, checksum2);
                PluginValidator.verifyChecksum(manifest, artifact3, checksum3);
            }).doesNotThrowAnyException();
        }
    }
}