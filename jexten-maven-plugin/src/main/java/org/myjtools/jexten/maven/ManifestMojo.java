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
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Mojo(name = "generate-manifest", defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
public class ManifestMojo extends AbstractMojo {


    public static final String LICENSE_FILE = "LICENSE";
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private File outputDir;

    @Parameter(property = "application")
    private String application;

    @Parameter(property = "hostModule")
    private String hostModule;

    @Parameter(property = "excludedDependencies")
    private List<String> excludedDependencies;



    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (!"jar".equals(project.getPackaging())) {
            return;
        }

        if (application == null || application.isBlank()) {
            throw new MojoExecutionException("Application name must be specified using the 'application' parameter.");
        }
        if (hostModule == null || hostModule.isBlank()) {
            throw new MojoExecutionException("Host module name must be specified using the 'hostModule' parameter.");
        }


        File licenseFile = new File(basedir, LICENSE_FILE);
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
            .licenseText(Files.readString(new File(basedir, LICENSE_FILE).toPath()))
            .application(application)
            .hostModule(hostModule)
            .url(project.getUrl())
            .artifacts(computeDependencies())
            .extensions(readExtensions())
            .extensionPoints(readExtensionPoints())
            .build();
        File manifestFile = new File(outputDir, PluginFile.PLUGIN_MANIFEST_FILE);
        try (var writer = Files.newBufferedWriter(manifestFile.toPath())) {
            manifest.write(writer);
        }
    }


    private Map<String, List<String>> readExtensions() throws IOException {
        Map<String,List<String>> result = new HashMap<>();
        File extensionsFile = new File(outputDir, "META-INF/extensions");
        if (extensionsFile.exists()) {
            List<String> lines = Files.readAllLines(extensionsFile.toPath());
            for (String line : lines) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String extensionPoint = parts[0].trim();
                    String extensions = parts[1].trim();
                    result.computeIfAbsent(extensionPoint, k -> new java.util.ArrayList<>())
                        .addAll(List.of(extensions.split(",")));
                } else {
                    getLog().warn("Invalid extension format: " + line);
                }
            }
         }
        return result;
    }


    private List<String> readExtensionPoints() throws IOException {
        File extensionPointsFile = new File(outputDir, "META-INF/extension-points");
        if (!extensionPointsFile.exists()) {
            return List.of();
        }
        return Files.readAllLines(extensionPointsFile.toPath());
    }


    private Map<String, List<String>> computeDependencies() {
        Map<String,List<String>> artifacts = new HashMap<>();
        for (var dependency : project.getDependencies()) {

            if ("compile".equals(dependency.getScope()) || "runtime".equals(dependency.getScope())) {
                if (isExcludedDependency(dependency.getGroupId(),dependency.getArtifactId())) {
                    continue;
                }
                artifacts.computeIfAbsent(dependency.getGroupId(), k -> new java.util.ArrayList<>())
                        .add(dependency.getArtifactId() + "-" + dependency.getVersion());
            }
        }
        artifacts.computeIfAbsent(project.getGroupId(), k -> new java.util.ArrayList<>())
                .add(project.getArtifactId() + "-" + project.getVersion());

        return artifacts;
    }


    private boolean isExcludedDependency(String groupId, String artifactId) {
        if (excludedDependencies == null) {
            return false;
        }
        for (String excludedDependency : excludedDependencies) {
            String excludedDependencyGroupId = excludedDependency.substring(0, excludedDependency.indexOf(':'));
            String excludedDependencyArtifactId = excludedDependency.substring(excludedDependency.indexOf(':') + 1);
            if (excludedDependencyGroupId.equals(groupId) && excludedDependencyArtifactId.equals(artifactId)) {
                return true;
            }
        }
        return false;
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
        assertNotEmpty(application, "Application name must not be null");
        assertNotEmpty(hostModule, "Host module name must not be null");
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
