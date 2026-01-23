package org.myjtools.jexten.plugin.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.myjtools.jexten.plugin.PluginException;
import org.myjtools.jexten.plugin.internal.FileUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.*;


@DisplayName("FileUtil Tests")
public class TestFileUtil {


    @Nested
    @DisplayName("Zip Slip Protection Tests")
    class ZipSlipProtectionTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("should allow normal zip entry paths")
        void shouldAllowNormalPaths() throws IOException {
            ZipEntry entry = new ZipEntry("normal/path/file.txt");
            Path result = FileUtil.zipSlipProtect(entry, tempDir);

            assertThat(result).isEqualTo(tempDir.resolve("normal/path/file.txt").normalize());
            assertThat(result.startsWith(tempDir)).isTrue();
        }

        @Test
        @DisplayName("should allow root-level file entries")
        void shouldAllowRootLevelFiles() throws IOException {
            ZipEntry entry = new ZipEntry("file.txt");
            Path result = FileUtil.zipSlipProtect(entry, tempDir);

            assertThat(result).isEqualTo(tempDir.resolve("file.txt"));
        }

        @Test
        @DisplayName("should block path traversal with ../ prefix")
        void shouldBlockPathTraversalWithDoubleDotPrefix() {
            ZipEntry entry = new ZipEntry("../../../etc/passwd");

            assertThatThrownBy(() -> FileUtil.zipSlipProtect(entry, tempDir))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Bad zip entry");
        }

        @Test
        @DisplayName("should block path traversal with embedded ../")
        void shouldBlockPathTraversalWithEmbeddedDoubleDot() throws IOException {
            // Create a subdirectory so the embedded ../ can escape it
            Path targetDir = tempDir.resolve("plugins");
            Files.createDirectories(targetDir);

            // This path tries to escape the plugins directory to write to tempDir directly
            ZipEntry entry = new ZipEntry("sub/../../../escape/malicious.txt");

            assertThatThrownBy(() -> FileUtil.zipSlipProtect(entry, targetDir))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Bad zip entry");
        }

        @Test
        @DisplayName("should block absolute path attempts on Unix")
        void shouldBlockAbsolutePathUnix() {
            // On Unix, /etc/passwd would be outside the target directory
            ZipEntry entry = new ZipEntry("/etc/passwd");
            Path targetDir = tempDir.resolve("plugins");

            // This path when resolved and normalized would still be /etc/passwd
            // which doesn't start with the target directory
            assertThatThrownBy(() -> FileUtil.zipSlipProtect(entry, targetDir))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Bad zip entry");
        }

        @Test
        @DisplayName("should allow paths with single dots")
        void shouldAllowPathsWithSingleDots() throws IOException {
            ZipEntry entry = new ZipEntry("./normal/./path/file.txt");
            Path result = FileUtil.zipSlipProtect(entry, tempDir);

            assertThat(result.startsWith(tempDir)).isTrue();
        }

        @Test
        @DisplayName("should handle deep nested paths correctly")
        void shouldHandleDeepNestedPaths() throws IOException {
            ZipEntry entry = new ZipEntry("a/b/c/d/e/f/g/h/i/j/file.txt");
            Path result = FileUtil.zipSlipProtect(entry, tempDir);

            assertThat(result.startsWith(tempDir)).isTrue();
            assertThat(result.getFileName().toString()).isEqualTo("file.txt");
        }

        @Test
        @DisplayName("should block backslash path traversal attempts")
        void shouldBlockBackslashPathTraversal() {
            // Some systems might try backslash traversal
            ZipEntry entry = new ZipEntry("..\\..\\etc\\passwd");

            // After normalization, if the path escapes the target, it should fail
            // Note: behavior may vary by OS, but the normalize() should handle it
            try {
                Path result = FileUtil.zipSlipProtect(entry, tempDir);
                // If it doesn't throw, ensure it's still within tempDir
                assertThat(result.startsWith(tempDir)).isTrue();
            } catch (IOException e) {
                assertThat(e.getMessage()).contains("Bad zip entry");
            }
        }
    }


    @Nested
    @DisplayName("Delete File Tests")
    class DeleteFileTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("should delete existing file")
        void shouldDeleteExistingFile() throws IOException {
            Path file = tempDir.resolve("test.txt");
            Files.writeString(file, "content");
            assertThat(file).exists();

            FileUtil.deleteFile(file);

            assertThat(file).doesNotExist();
        }

        @Test
        @DisplayName("should handle non-existent file gracefully")
        void shouldHandleNonExistentFile() {
            Path file = tempDir.resolve("nonexistent.txt");

            assertThatCode(() -> FileUtil.deleteFile(file))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("should handle null path gracefully")
        void shouldHandleNullPath() {
            assertThatCode(() -> FileUtil.deleteFile(null))
                .doesNotThrowAnyException();
        }
    }


    @Nested
    @DisplayName("Unzip File Tests")
    class UnzipFileTests {

        @TempDir
        Path tempDir;

        @Test
        @DisplayName("should extract file from zip")
        void shouldExtractFileFromZip() throws IOException {
            // Create a test zip file
            Path zipPath = tempDir.resolve("test.zip");
            String content = "Hello, World!";

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                ZipEntry entry = new ZipEntry("test.txt");
                zos.putNextEntry(entry);
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            // Extract
            Path extractedPath = tempDir.resolve("extracted/test.txt");
            try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                ZipEntry entry = zipFile.getEntry("test.txt");
                FileUtil.unzipFile(zipFile, entry, extractedPath);
            }

            assertThat(extractedPath).exists();
            assertThat(Files.readString(extractedPath)).isEqualTo(content);
        }

        @Test
        @DisplayName("should create parent directories if needed")
        void shouldCreateParentDirectories() throws IOException {
            Path zipPath = tempDir.resolve("test.zip");
            String content = "Nested content";

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                ZipEntry entry = new ZipEntry("deep/nested/path/file.txt");
                zos.putNextEntry(entry);
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            Path extractedPath = tempDir.resolve("out/deep/nested/path/file.txt");
            try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                ZipEntry entry = zipFile.getEntry("deep/nested/path/file.txt");
                FileUtil.unzipFile(zipFile, entry, extractedPath);
            }

            assertThat(extractedPath).exists();
            assertThat(extractedPath.getParent()).exists();
        }

        @Test
        @DisplayName("should overwrite existing file")
        void shouldOverwriteExistingFile() throws IOException {
            Path zipPath = tempDir.resolve("test.zip");
            String newContent = "New content";

            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                ZipEntry entry = new ZipEntry("file.txt");
                zos.putNextEntry(entry);
                zos.write(newContent.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            Path extractedPath = tempDir.resolve("file.txt");
            Files.writeString(extractedPath, "Old content");

            try (ZipFile zipFile = new ZipFile(zipPath.toFile())) {
                ZipEntry entry = zipFile.getEntry("file.txt");
                FileUtil.unzipFile(zipFile, entry, extractedPath);
            }

            assertThat(Files.readString(extractedPath)).isEqualTo(newContent);
        }
    }


    @Nested
    @DisplayName("Find Artifact Name Tests")
    class FindArtifactNameTests {

        @Test
        @DisplayName("should extract artifact name from standard jar path")
        void shouldExtractNameFromStandardJar() {
            Path artifact = Path.of("repo/org/example/my-artifact-1.0.0.jar");
            String name = FileUtil.findArtifactName(artifact);
            assertThat(name).isEqualTo("my-artifact");
        }

        @Test
        @DisplayName("should extract artifact name with multiple hyphens")
        void shouldExtractNameWithMultipleHyphens() {
            Path artifact = Path.of("my-multi-part-artifact-2.3.4.jar");
            String name = FileUtil.findArtifactName(artifact);
            assertThat(name).isEqualTo("my-multi-part-artifact");
        }

        @Test
        @DisplayName("should handle snapshot versions")
        void shouldHandleSnapshotVersions() {
            Path artifact = Path.of("my-artifact-1.0.0-SNAPSHOT.jar");
            String name = FileUtil.findArtifactName(artifact);
            // The method looks for the last hyphen, so SNAPSHOT will be included in version
            assertThat(name).isEqualTo("my-artifact-1.0.0");
        }

        @Test
        @DisplayName("should throw for filename without hyphen")
        void shouldThrowForFilenameWithoutHyphen() {
            Path artifact = Path.of("myartifact.jar");

            assertThatThrownBy(() -> FileUtil.findArtifactName(artifact))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid artifact filename");
        }

        @Test
        @DisplayName("should handle simple name-version format")
        void shouldHandleSimpleNameVersion() {
            Path artifact = Path.of("commons-1.0.jar");
            String name = FileUtil.findArtifactName(artifact);
            assertThat(name).isEqualTo("commons");
        }
    }


    @Nested
    @DisplayName("Find Artifact Version Tests")
    class FindArtifactVersionTests {

        @Test
        @DisplayName("should extract version from standard jar path")
        void shouldExtractVersionFromStandardJar() {
            Path artifact = Path.of("my-artifact-1.0.0.jar");
            String version = FileUtil.findArtifactVersion(artifact);
            assertThat(version).isEqualTo("1.0.0");
        }

        @Test
        @DisplayName("should extract version with multiple digits")
        void shouldExtractVersionWithMultipleDigits() {
            Path artifact = Path.of("library-12.345.678.jar");
            String version = FileUtil.findArtifactVersion(artifact);
            assertThat(version).isEqualTo("12.345.678");
        }

        @Test
        @DisplayName("should handle SNAPSHOT suffix in version")
        void shouldHandleSnapshotSuffix() {
            Path artifact = Path.of("my-lib-1.0.0-SNAPSHOT.jar");
            String version = FileUtil.findArtifactVersion(artifact);
            // Last hyphen is used, so version is "SNAPSHOT"
            assertThat(version).isEqualTo("SNAPSHOT");
        }

        @Test
        @DisplayName("should throw for filename without hyphen")
        void shouldThrowForFilenameWithoutHyphen() {
            Path artifact = Path.of("myartifact.jar");

            assertThatThrownBy(() -> FileUtil.findArtifactVersion(artifact))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalid artifact filename");
        }

        @Test
        @DisplayName("should strip .jar extension")
        void shouldStripJarExtension() {
            Path artifact = Path.of("test-lib-2.0.jar");
            String version = FileUtil.findArtifactVersion(artifact);
            assertThat(version).isEqualTo("2.0");
            assertThat(version).doesNotContain(".jar");
        }
    }


    @Nested
    @DisplayName("YAML Writer Tests")
    class YamlWriterTests {

        @Test
        @DisplayName("should create yaml writer with block flow style")
        void shouldCreateYamlWriterWithBlockStyle() {
            Yaml yaml = FileUtil.yamlWriter();
            assertThat(yaml).isNotNull();
        }

        @Test
        @DisplayName("should serialize map to yaml in block style")
        void shouldSerializeMapInBlockStyle() {
            Yaml yaml = FileUtil.yamlWriter();

            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("name", "test");
            data.put("number", 42);

            String result = yaml.dump(data);

            assertThat(result).contains("name: test");
            assertThat(result).contains("number: 42");
        }

        @Test
        @DisplayName("should serialize nested maps correctly")
        void shouldSerializeNestedMaps() {
            Yaml yaml = FileUtil.yamlWriter();

            Map<String, Object> nested = new java.util.LinkedHashMap<>();
            nested.put("key", "value");

            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("name", "test");
            data.put("metadata", nested);

            String result = yaml.dump(data);

            assertThat(result).contains("name: test");
            assertThat(result).contains("metadata");
            assertThat(result).contains("key: value");
        }

        @Test
        @DisplayName("should use pretty formatting with indentation")
        void shouldUsePrettyFormatting() {
            Yaml yaml = FileUtil.yamlWriter();

            Map<String, Object> nested = new java.util.LinkedHashMap<>();
            nested.put("innerKey", "innerValue");

            Map<String, Object> data = new java.util.LinkedHashMap<>();
            data.put("outer", nested);

            String result = yaml.dump(data);

            // Should have proper indentation (2 spaces as configured)
            assertThat(result).contains("outer:");
            // Block style should have nested content on new lines
            assertThat(result.split("\n").length).isGreaterThan(1);
        }
    }
}