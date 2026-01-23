package org.myjtools.jexten.maven;

import org.apache.maven.model.License;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
@DisplayName("ManifestMojo")
class ManifestMojoTest {

    @Mock
    private MavenProject project;

    private ManifestMojo mojo;

    @TempDir
    Path tempDir;

    private File basedir;
    private File outputDir;

    @BeforeEach
    void setUp() throws Exception {
        mojo = new ManifestMojo();
        basedir = tempDir.toFile();
        outputDir = tempDir.resolve("target/classes").toFile();
        outputDir.mkdirs();

        setField(mojo, "project", project);
        setField(mojo, "basedir", basedir);
        setField(mojo, "outputDir", outputDir);
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
            Assertions.assertDoesNotThrow(()->mojo.execute());
        }


        @Test
        @DisplayName("should process jar packaging")
        void shouldProcessJarPackaging()  {
            when(project.getPackaging()).thenReturn("jar");

            // Should throw because application is not set
            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("application");
        }
    }


    @Nested
    @DisplayName("Required parameters validation")
    class RequiredParametersValidation {

        @BeforeEach
        void setUp() {
            when(project.getPackaging()).thenReturn("jar");
        }


        @Test
        @DisplayName("should fail when application is null")
        void shouldFailWhenApplicationIsNull() throws Exception {
            setField(mojo, "application", null);

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Application name must be specified");
        }


        @Test
        @DisplayName("should fail when application is blank")
        void shouldFailWhenApplicationIsBlank() throws Exception {
            setField(mojo, "application", "   ");

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("Application name must be specified");
        }


        void validateModule(String module) throws Exception {
            setField(mojo, "application", "com.example.app");
            setField(mojo, "hostModule", module);

            assertThatThrownBy(() -> mojo.execute())
                    .isInstanceOf(MojoExecutionException.class)
                    .hasMessageContaining("Host module name must be specified");
        }

        @Test
        @DisplayName("should fail when hostModule is null")
        void shouldFailWhenHostModuleIsNull() throws Exception {
            validateModule(null);
        }


        @Test
        @DisplayName("should fail when hostModule is blank")
        void shouldFailWhenHostModuleIsBlank() throws Exception {
            validateModule("");
        }


        @Test
        @DisplayName("should fail when LICENSE file does not exist")
        void shouldFailWhenLicenseFileNotFound() throws Exception {
            setField(mojo, "application", "com.example.app");
            setField(mojo, "hostModule", "com.example.host");

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("License file not found");
        }
    }


    @Nested
    @DisplayName("Project validation")
    class ProjectValidation {

        @BeforeEach
        void setUp() throws Exception {
            when(project.getPackaging()).thenReturn("jar");
            setField(mojo, "application", "com.example.app");
            setField(mojo, "hostModule", "com.example.host");

            // Create LICENSE file
            Files.writeString(tempDir.resolve("LICENSE"), "MIT License");
        }


        @Test
        @DisplayName("should fail when project groupId is null")
        void shouldFailWhenGroupIdIsNull() {
            when(project.getGroupId()).thenReturn(null);

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("groupId");
        }


        @Test
        @DisplayName("should fail when project artifactId is empty")
        void shouldFailWhenArtifactIdIsEmpty() {
            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("");

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("artifactId");
        }


        @Test
        @DisplayName("should fail when project version is null")
        void shouldFailWhenVersionIsNull() {
            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn(null);

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("version");
        }


        @Test
        @DisplayName("should fail when project name is null")
        void shouldFailWhenNameIsNull() {
            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");
            when(project.getName()).thenReturn(null);

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("name");
        }


        @Test
        @DisplayName("should fail when project description is null")
        void shouldFailWhenDescriptionIsNull() {
            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");
            when(project.getName()).thenReturn("My Plugin");
            when(project.getDescription()).thenReturn(null);

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("description");
        }


        @Test
        @DisplayName("should fail when project has no licenses")
        void shouldFailWhenNoLicenses() {
            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");
            when(project.getName()).thenReturn("My Plugin");
            when(project.getDescription()).thenReturn("A test plugin");
            when(project.getLicenses()).thenReturn(Collections.emptyList());

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("licenses");
        }


        @Test
        @DisplayName("should fail when project URL is null")
        void shouldFailWhenUrlIsNull() {
            License license = new License();
            license.setName("MIT");

            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");
            when(project.getName()).thenReturn("My Plugin");
            when(project.getDescription()).thenReturn("A test plugin");
            when(project.getLicenses()).thenReturn(List.of(license));
            when(project.getUrl()).thenReturn(null);

            assertThatThrownBy(() -> mojo.execute())
                .isInstanceOf(MojoExecutionException.class)
                .hasMessageContaining("URL");
        }
    }


    @Nested
    @DisplayName("Manifest generation")
    class ManifestGeneration {

        @BeforeEach
        void setUp() throws Exception {
            when(project.getPackaging()).thenReturn("jar");
            setField(mojo, "application", "com.example.app");
            setField(mojo, "hostModule", "com.example.host");

            // Create LICENSE file
            Files.writeString(tempDir.resolve("LICENSE"), "MIT License\nCopyright 2024");
        }


        @Test
        @DisplayName("should generate plugin.yaml manifest")
        void shouldGenerateManifest() throws Exception {
            License license = new License();
            license.setName("MIT");

            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");
            when(project.getName()).thenReturn("My Plugin");
            when(project.getDescription()).thenReturn("A test plugin");
            when(project.getLicenses()).thenReturn(List.of(license));
            when(project.getUrl()).thenReturn("https://example.com");
            when(project.getDependencies()).thenReturn(Collections.emptyList());

            mojo.execute();

            File manifestFile = new File(outputDir, "plugin.yaml");
            assertThat(manifestFile).exists();

            String content = Files.readString(manifestFile.toPath());
            assertThat(content).contains("application: com.example.app")
            .contains("group: com.example")
            .contains("name: my-plugin")
            .contains("version:")
            .contains("displayName: My Plugin")
            .contains("description: A test plugin")
            .contains("licenseName: MIT")
            .contains("url: https://example.com")
            .contains("hostModule: com.example.host");
        }


        @Test
        @DisplayName("should include license text in manifest")
        void shouldIncludeLicenseText() throws Exception {
            License license = new License();
            license.setName("Apache-2.0");

            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");
            when(project.getName()).thenReturn("My Plugin");
            when(project.getDescription()).thenReturn("A test plugin");
            when(project.getLicenses()).thenReturn(List.of(license));
            when(project.getUrl()).thenReturn("https://example.com");
            when(project.getDependencies()).thenReturn(Collections.emptyList());

            mojo.execute();

            File manifestFile = new File(outputDir, "plugin.yaml");
            String content = Files.readString(manifestFile.toPath());
            assertThat(content).contains("licenseText:").contains("MIT License");
        }


        @Test
        @DisplayName("should read extensions from META-INF/extensions")
        void shouldReadExtensions() throws Exception {
            License license = new License();
            license.setName("MIT");

            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");
            when(project.getName()).thenReturn("My Plugin");
            when(project.getDescription()).thenReturn("A test plugin");
            when(project.getLicenses()).thenReturn(List.of(license));
            when(project.getUrl()).thenReturn("https://example.com");
            when(project.getDependencies()).thenReturn(Collections.emptyList());

            // Create META-INF/extensions file
            File metaInf = new File(outputDir, "META-INF");
            metaInf.mkdirs();
            Files.writeString(metaInf.toPath().resolve("extensions"),
                "com.example.Greeter=com.example.impl.GreeterImpl,com.example.impl.AnotherGreeter");

            mojo.execute();

            File manifestFile = new File(outputDir, "plugin.yaml");
            String content = Files.readString(manifestFile.toPath());
            assertThat(content).contains("extensions:")
            .contains("com.example.Greeter:")
            .contains("com.example.impl.GreeterImpl")
            .contains("com.example.impl.AnotherGreeter");
        }


        @Test
        @DisplayName("should read extension points from META-INF/extension-points")
        void shouldReadExtensionPoints() throws Exception {
            License license = new License();
            license.setName("MIT");

            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");
            when(project.getName()).thenReturn("My Plugin");
            when(project.getDescription()).thenReturn("A test plugin");
            when(project.getLicenses()).thenReturn(List.of(license));
            when(project.getUrl()).thenReturn("https://example.com");
            when(project.getDependencies()).thenReturn(Collections.emptyList());

            // Create META-INF/extension-points file
            File metaInf = new File(outputDir, "META-INF");
            metaInf.mkdirs();
            Files.writeString(metaInf.toPath().resolve("extension-points"),
                "com.example.Service\ncom.example.Repository");

            mojo.execute();

            File manifestFile = new File(outputDir, "plugin.yaml");
            String content = Files.readString(manifestFile.toPath());
            assertThat(content).contains("extensionPoints:")
            .contains("com.example.Service")
            .contains("com.example.Repository");
        }
    }


    @Nested
    @DisplayName("Dependency computation")
    class DependencyComputation {

        @BeforeEach
        void setUp() throws Exception {
            when(project.getPackaging()).thenReturn("jar");
            setField(mojo, "application", "com.example.app");
            setField(mojo, "hostModule", "com.example.host");
            setField(mojo, "excludedDependencies", List.of("com.example:host-app"));

            Files.writeString(tempDir.resolve("LICENSE"), "MIT License");
        }


        @Test
        @DisplayName("should include compile dependencies")
        void shouldIncludeCompileDependencies() throws Exception {
            License license = new License();
            license.setName("MIT");

            org.apache.maven.model.Dependency dep = new org.apache.maven.model.Dependency();
            dep.setGroupId("org.apache.commons");
            dep.setArtifactId("commons-lang3");
            dep.setVersion("3.14.0");
            dep.setScope("compile");

            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");
            when(project.getName()).thenReturn("My Plugin");
            when(project.getDescription()).thenReturn("A test plugin");
            when(project.getLicenses()).thenReturn(List.of(license));
            when(project.getUrl()).thenReturn("https://example.com");
            when(project.getDependencies()).thenReturn(List.of(dep));

            mojo.execute();

            File manifestFile = new File(outputDir, "plugin.yaml");
            String content = Files.readString(manifestFile.toPath());
            assertThat(content).contains("artifacts:")
            .contains("org.apache.commons:")
            .contains("commons-lang3-3.14.0");
        }


        @Test
        @DisplayName("should exclude test dependencies")
        void shouldExcludeTestDependencies() throws Exception {
            License license = new License();
            license.setName("MIT");

            org.apache.maven.model.Dependency testDep = new org.apache.maven.model.Dependency();
            testDep.setGroupId("org.junit.jupiter");
            testDep.setArtifactId("junit-jupiter");
            testDep.setVersion("5.10.0");
            testDep.setScope("test");

            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");
            when(project.getName()).thenReturn("My Plugin");
            when(project.getDescription()).thenReturn("A test plugin");
            when(project.getLicenses()).thenReturn(List.of(license));
            when(project.getUrl()).thenReturn("https://example.com");
            when(project.getDependencies()).thenReturn(List.of(testDep));

            mojo.execute();

            File manifestFile = new File(outputDir, "plugin.yaml");
            String content = Files.readString(manifestFile.toPath());
            assertThat(content).doesNotContain("junit-jupiter");
        }



        @Test
        @DisplayName("should include runtime dependencies")
        void shouldIncludeRuntimeDependencies() throws Exception {
            License license = new License();
            license.setName("MIT");

            org.apache.maven.model.Dependency runtimeDep = new org.apache.maven.model.Dependency();
            runtimeDep.setGroupId("org.slf4j");
            runtimeDep.setArtifactId("slf4j-simple");
            runtimeDep.setVersion("2.0.9");
            runtimeDep.setScope("runtime");

            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");
            when(project.getName()).thenReturn("My Plugin");
            when(project.getDescription()).thenReturn("A test plugin");
            when(project.getLicenses()).thenReturn(List.of(license));
            when(project.getUrl()).thenReturn("https://example.com");
            when(project.getDependencies()).thenReturn(List.of(runtimeDep));

            mojo.execute();

            File manifestFile = new File(outputDir, "plugin.yaml");
            String content = Files.readString(manifestFile.toPath());
            assertThat(content).contains("slf4j-simple-2.0.9");
        }


        @Test
        @DisplayName("should include project artifact itself")
        void shouldIncludeProjectArtifact() throws Exception {
            License license = new License();
            license.setName("MIT");

            when(project.getGroupId()).thenReturn("com.example");
            when(project.getArtifactId()).thenReturn("my-plugin");
            when(project.getVersion()).thenReturn("1.0.0");
            when(project.getName()).thenReturn("My Plugin");
            when(project.getDescription()).thenReturn("A test plugin");
            when(project.getLicenses()).thenReturn(List.of(license));
            when(project.getUrl()).thenReturn("https://example.com");
            when(project.getDependencies()).thenReturn(Collections.emptyList());

            mojo.execute();

            File manifestFile = new File(outputDir, "plugin.yaml");
            String content = Files.readString(manifestFile.toPath());
            assertThat(content).contains("my-plugin-1.0.0");
        }
    }
}
