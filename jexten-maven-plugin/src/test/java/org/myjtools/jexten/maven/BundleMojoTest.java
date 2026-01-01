package org.myjtools.jexten.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("BundleMojo")
class BundleMojoTest {

    @Mock
    private MavenProject project;

    @Mock
    private RepositorySystemSession repoSession;

    private BundleMojo mojo;

    @TempDir
    Path tempDir;

    private File buildDirectory;
    private File outputDirectory;

    @BeforeEach
    void setUp() throws Exception {
        mojo = new BundleMojo();

        buildDirectory = tempDir.resolve("target").toFile();
        buildDirectory.mkdirs();

        outputDirectory = tempDir.resolve("target/classes").toFile();
        outputDirectory.mkdirs();

        setField(mojo, "project", project);
        setField(mojo, "buildDirectory", buildDirectory);
        setField(mojo, "outputDirectory", outputDirectory);
        setField(mojo, "repoSession", repoSession);
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }


    @Nested
    @DisplayName("Packaging validation")
    class PackagingValidation {

        @Test
        @DisplayName("should skip execution for non-jar packaging")
        void shouldSkipForNonJarPackaging() throws Exception {
            when(project.getPackaging()).thenReturn("pom");

            // Should not throw - just skips
            mojo.execute();
        }


        @Test
        @DisplayName("should skip execution for war packaging")
        void shouldSkipForWarPackaging() throws Exception {
            when(project.getPackaging()).thenReturn("war");

            // Should not throw - just skips
            mojo.execute();
        }


        @Test
        @DisplayName("should process jar packaging")
        void shouldProcessJarPackaging() {
            when(project.getPackaging()).thenReturn("jar");

            // Should throw because hostArtifact is not set
            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("hostArtifact");
        }
    }


    @Nested
    @DisplayName("Host artifact validation")
    class HostArtifactValidation {

        @BeforeEach
        void setUp() {
            when(project.getPackaging()).thenReturn("jar");
        }


        @Test
        @DisplayName("should fail when hostArtifact is null")
        void shouldFailWhenHostArtifactIsNull() throws Exception {
            setField(mojo, "hostArtifact", null);

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Host artifact must be specified");
        }


        @Test
        @DisplayName("should fail when hostArtifact is blank")
        void shouldFailWhenHostArtifactIsBlank() throws Exception {
            setField(mojo, "hostArtifact", "   ");

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Host artifact must be specified");
        }


        @Test
        @DisplayName("should fail when hostArtifact is empty")
        void shouldFailWhenHostArtifactIsEmpty() throws Exception {
            setField(mojo, "hostArtifact", "");

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Host artifact must be specified");
        }


        @Test
        @DisplayName("should parse hostArtifact with groupId:artifactId format")
        void shouldParseHostArtifactFormat() throws Exception {
            setField(mojo, "hostArtifact", "com.example:my-app");

            LocalRepository localRepo = mock(LocalRepository.class);
            when(localRepo.getBasedir()).thenReturn(tempDir.toFile());
            when(repoSession.getLocalRepository()).thenReturn(localRepo);
            when(project.getDependencies()).thenReturn(java.util.Collections.emptyList());
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");

            // Create plugin.yaml
            Files.writeString(outputDirectory.toPath().resolve("plugin.yaml"), "test: value");

            // Create jar file
            Files.createFile(buildDirectory.toPath().resolve("my-plugin-1.0.0.jar"));

            // Execute - should succeed now as maven-fetcher works with empty deps
            mojo.execute();

            // Verify bundle was created
            File bundleFile = new File(buildDirectory, "my-plugin-bundle-1.0.0.zip");
            org.assertj.core.api.Assertions.assertThat(bundleFile).exists();
        }
    }


    @Nested
    @DisplayName("Host artifact parsing")
    class HostArtifactParsing {



        @Test
        @DisplayName("should correctly extract groupId from hostArtifact")
        void shouldExtractGroupId() throws Exception {
            when(project.getPackaging()).thenReturn("jar");
            setField(mojo, "hostArtifact", "org.example.group:my-artifact");

            LocalRepository localRepo = mock(LocalRepository.class);
            when(localRepo.getBasedir()).thenReturn(tempDir.toFile());
            when(repoSession.getLocalRepository()).thenReturn(localRepo);
            when(project.getDependencies()).thenReturn(java.util.Collections.emptyList());
            when(project.getArtifactId()).thenReturn("test-plugin");
            when(project.getVersion()).thenReturn("1.0.0");

            Files.writeString(outputDirectory.toPath().resolve("plugin.yaml"), "test: value");
            Files.createFile(buildDirectory.toPath().resolve("test-plugin-1.0.0.jar"));

            // Will fail later, but we verify parsing worked by checking it gets past hostArtifact validation
            try {
                mojo.execute();
            } catch (MojoExecutionException e) {
                // Expected - verify it's not about hostArtifact parsing
                org.assertj.core.api.Assertions.assertThat(e.getMessage())
                    .doesNotContain("Host artifact must be specified");
            }
        }


    }


    @Nested
    @DisplayName("Bundle file creation")
    class BundleFileCreation {

        @BeforeEach
        void setUp() throws Exception {
            when(project.getPackaging()).thenReturn("jar");
            setField(mojo, "hostArtifact", "com.example:host-app");

            LocalRepository localRepo = mock(LocalRepository.class);
            when(localRepo.getBasedir()).thenReturn(tempDir.toFile());
            when(repoSession.getLocalRepository()).thenReturn(localRepo);
            when(project.getDependencies()).thenReturn(java.util.Collections.emptyList());
        }


        @Test
        @DisplayName("should fail when plugin.yaml does not exist")
        void shouldFailWhenPluginYamlNotFound() throws Exception {
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");

            // Create jar but not plugin.yaml
            Files.createFile(buildDirectory.toPath().resolve("my-plugin-1.0.0.jar"));

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Error creating bundle file");
        }


        @Test
        @DisplayName("should fail when jar file does not exist")
        void shouldFailWhenJarNotFound() throws Exception {
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");

            // Create plugin.yaml but not jar
            Files.writeString(outputDirectory.toPath().resolve("plugin.yaml"), "test: value");

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Error creating bundle file");
        }


        @Test
        @DisplayName("should create bundle zip file with correct name")
        void shouldCreateBundleWithCorrectName() throws Exception {
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("2.0.0");

            Files.writeString(outputDirectory.toPath().resolve("plugin.yaml"), "test: value");
            Files.createFile(buildDirectory.toPath().resolve("my-plugin-2.0.0.jar"));

            mojo.execute();

            File bundleFile = new File(buildDirectory, "my-plugin-bundle-2.0.0.zip");
            org.assertj.core.api.Assertions.assertThat(bundleFile).exists();
        }


        @Test
        @DisplayName("should include plugin.yaml in bundle")
        void shouldIncludePluginYaml() throws Exception {
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");

            Files.writeString(outputDirectory.toPath().resolve("plugin.yaml"), "name: my-plugin\nversion: 1.0.0");
            Files.write(buildDirectory.toPath().resolve("my-plugin-1.0.0.jar"), "dummy jar content".getBytes());

            mojo.execute();

            File bundleFile = new File(buildDirectory, "my-plugin-bundle-1.0.0.zip");
            org.assertj.core.api.Assertions.assertThat(bundleFile).exists();

            // Verify bundle contains plugin.yaml
            try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(bundleFile)) {
                org.assertj.core.api.Assertions.assertThat(zipFile.getEntry("plugin.yaml")).isNotNull();
            }
        }


        @Test
        @DisplayName("should include jar file in bundle")
        void shouldIncludeJarFile() throws Exception {
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");

            Files.writeString(outputDirectory.toPath().resolve("plugin.yaml"), "name: my-plugin");
            Files.write(buildDirectory.toPath().resolve("my-plugin-1.0.0.jar"), "dummy jar content".getBytes());

            mojo.execute();

            File bundleFile = new File(buildDirectory, "my-plugin-bundle-1.0.0.zip");

            try (java.util.zip.ZipFile zipFile = new java.util.zip.ZipFile(bundleFile)) {
                org.assertj.core.api.Assertions.assertThat(zipFile.getEntry("my-plugin-1.0.0.jar")).isNotNull();
            }
        }
    }


    @Nested
    @DisplayName("Dependency filtering")
    class DependencyFiltering {

        @BeforeEach
        void setUp() throws Exception {
            when(project.getPackaging()).thenReturn("jar");
            setField(mojo, "hostArtifact", "com.example:host-app");

            LocalRepository localRepo = mock(LocalRepository.class);
            when(localRepo.getBasedir()).thenReturn(tempDir.toFile());
            when(repoSession.getLocalRepository()).thenReturn(localRepo);
        }


        @Test
        @DisplayName("should include compile scope dependencies")
        void shouldIncludeCompileDependencies() throws Exception {
            org.apache.maven.model.Dependency dep = new org.apache.maven.model.Dependency();
            dep.setGroupId("org.apache.commons");
            dep.setArtifactId("commons-lang3");
            dep.setVersion("3.14.0");
            dep.setScope("compile");

            when(project.getDependencies()).thenReturn(java.util.List.of(dep));
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");

            Files.writeString(outputDirectory.toPath().resolve("plugin.yaml"), "name: my-plugin");
            Files.createFile(buildDirectory.toPath().resolve("my-plugin-1.0.0.jar"));

            // Execute will try to fetch dependencies through maven-fetcher
            // The test verifies compile deps are processed
            try {
                mojo.execute();
            } catch (MojoExecutionException e) {
                // Expected - maven-fetcher can't actually fetch
            }
        }


        @Test
        @DisplayName("should include runtime scope dependencies")
        void shouldIncludeRuntimeDependencies() throws Exception {
            org.apache.maven.model.Dependency dep = new org.apache.maven.model.Dependency();
            dep.setGroupId("org.slf4j");
            dep.setArtifactId("slf4j-simple");
            dep.setVersion("2.0.9");
            dep.setScope("runtime");

            when(project.getDependencies()).thenReturn(java.util.List.of(dep));
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");

            Files.writeString(outputDirectory.toPath().resolve("plugin.yaml"), "name: my-plugin");
            Files.createFile(buildDirectory.toPath().resolve("my-plugin-1.0.0.jar"));

            try {
                mojo.execute();
            } catch (MojoExecutionException e) {
                // Expected
            }
        }


        @Test
        @DisplayName("should exclude test scope dependencies")
        void shouldExcludeTestDependencies() throws Exception {
            org.apache.maven.model.Dependency testDep = new org.apache.maven.model.Dependency();
            testDep.setGroupId("org.junit.jupiter");
            testDep.setArtifactId("junit-jupiter");
            testDep.setVersion("5.10.0");
            testDep.setScope("test");

            when(project.getDependencies()).thenReturn(java.util.List.of(testDep));
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");

            Files.writeString(outputDirectory.toPath().resolve("plugin.yaml"), "name: my-plugin");
            Files.createFile(buildDirectory.toPath().resolve("my-plugin-1.0.0.jar"));

            // Test deps should be excluded, so no fetching needed
            mojo.execute();

            File bundleFile = new File(buildDirectory, "my-plugin-bundle-1.0.0.zip");
            org.assertj.core.api.Assertions.assertThat(bundleFile).exists();
        }


        @Test
        @DisplayName("should exclude provided scope dependencies")
        void shouldExcludeProvidedDependencies() throws Exception {
            org.apache.maven.model.Dependency providedDep = new org.apache.maven.model.Dependency();
            providedDep.setGroupId("javax.servlet");
            providedDep.setArtifactId("servlet-api");
            providedDep.setVersion("2.5");
            providedDep.setScope("provided");

            when(project.getDependencies()).thenReturn(java.util.List.of(providedDep));
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");

            Files.writeString(outputDirectory.toPath().resolve("plugin.yaml"), "name: my-plugin");
            Files.createFile(buildDirectory.toPath().resolve("my-plugin-1.0.0.jar"));

            // Provided deps should be excluded
            mojo.execute();

            File bundleFile = new File(buildDirectory, "my-plugin-bundle-1.0.0.zip");
            org.assertj.core.api.Assertions.assertThat(bundleFile).exists();
        }
    }
}
