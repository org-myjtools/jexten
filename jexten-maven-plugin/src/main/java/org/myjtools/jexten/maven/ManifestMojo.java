package org.myjtools.jexten.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.myjtools.jexten.plugin.PluginFile;
import org.myjtools.jexten.plugin.PluginManifest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;


@Mojo(name = "generate-manifest", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class ManifestMojo extends AbstractMojo {


    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(defaultValue = "${project.build.directory}", required = true)
    private File buildDir;

    @Parameter(property = "application", required = true)
    private String application;


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        File licenseFile = new File(basedir, "LICENSE");
        if (!licenseFile.exists()) {
            throw new MojoExecutionException("License file not found: " + licenseFile.getAbsolutePath());
        }
        validateProject(project);
        try {
            writePluginManifest();
        } catch (IOException e) {
            throw new MojoExecutionException(e);
        }
    }


    private void writePluginManifest() throws IOException {
        PluginManifest manifest = PluginManifest.builder()
            .displayName(project.getName())
            .group(project.getGroupId())
            .name(project.getArtifactId())
            .version(project.getVersion())
            .description(project.getDescription())
            .licenseName(project.getLicenses().getFirst().getName())
            .licenseText(Files.readString(new File(basedir, "LICENSE").toPath()))
            .application(application)
            .url(project.getUrl())
            .build();
        File manifestFile = new File(buildDir, PluginFile.PLUGIN_MANIFEST_FILE);
        try (var writer = Files.newBufferedWriter(manifestFile.toPath())) {
            manifest.write(writer);
        }
    }





    private void validateProject(MavenProject project) throws MojoExecutionException {
        assertNotNull(project, "Project must not be null");
        assertNotEmpty(project.getGroupId(), "Project groupId must not be null");
        assertNotEmpty(project.getArtifactId(), "Project artifactId must not be null");
        assertNotEmpty(project.getVersion(), "Project version must not be null");
        assertNotEmpty(project.getName(), "Project name must not be null");
        assertNotEmpty(project.getDescription(), "Project description must not be null");
        assertNotEmpty(project.getLicenses(), "Project licenses must not be null");
        assertNotEmpty(project.getLicenses().getFirst().getName(), "Project license name must not be null");
        assertNotEmpty(project.getUrl(), "Project URL must not be null");
    }



    private void assertNotEmpty(String value, String message) throws MojoExecutionException {
        if (value == null || value.isEmpty()) {
            throw new MojoExecutionException(message);
        }
    }

    private void assertNotNull(MavenProject project, String message) throws MojoExecutionException {
        if (project == null) {
            throw new MojoExecutionException(message);
        }
    }

    private void assertNotEmpty(List<?> values, String message) throws MojoExecutionException {
        if (values == null || values.isEmpty()) {
            throw new MojoExecutionException(message);
        }
    }
}
