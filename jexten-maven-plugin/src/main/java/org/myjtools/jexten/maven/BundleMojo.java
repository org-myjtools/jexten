package org.myjtools.jexten.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.myjtools.jexten.plugin.PluginValidator;
import org.myjtools.mavenfetcher.FetchedArtifact;
import org.myjtools.mavenfetcher.MavenFetchRequest;
import org.myjtools.mavenfetcher.MavenFetchResult;
import org.myjtools.mavenfetcher.MavenFetcher;

import org.myjtools.jexten.plugin.PluginFile;
import org.myjtools.jexten.plugin.PluginManifest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Mojo(name = "assemble-bundle", defaultPhase = LifecyclePhase.PACKAGE)
public class BundleMojo extends AbstractMojo {


    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    protected RepositorySystemSession repoSession;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private File buildDirectory;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
    private File outputDirectory;

    @Parameter(property = "hostArtifact")
    private String hostArtifact;

    private String hostGroupId;
    private String hostArtifactId;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (!"jar".equals(project.getPackaging())) {
            return;
        }

        if (hostArtifact == null || hostArtifact.isBlank()) {
            throw new MojoExecutionException("Host artifact must be specified using the 'hostArtifact' parameter.");
        }

        hostGroupId = hostArtifact.substring(0, hostArtifact.indexOf(':'));
        hostArtifactId = hostArtifact.substring(hostArtifact.indexOf(':') + 1);


        Set<String> artifacts = new HashSet<>();
        for (var dependency : project.getDependencies()) {
            if (dependency.getScope().equals("compile") || dependency.getScope().equals("runtime")) {
                String artifact = dependency.getGroupId()+":"+dependency.getArtifactId()+":"+dependency.getVersion();
                artifacts.add(artifact);
            }
        }
        MavenFetchResult fetchResult = new MavenFetcher()
            .localRepositoryPath(repoSession.getLocalRepository().getBasedir().toPath())
            .fetchArtifacts(new MavenFetchRequest(artifacts).scopes("compile", "runtime"));


        File bundleFile = new File(buildDirectory, project.getArtifactId()+"-bundle-"+project.getVersion() + ".zip");
        getLog().info("Creating bundle file: " + bundleFile.getAbsolutePath());

        try (var zipOut = new ZipOutputStream(new FileOutputStream(bundleFile))) {
            // Collect all artifacts to include
            File jar = new File(buildDirectory, project.getArtifactId() + "-" + project.getVersion() + ".jar");
            Set<FetchedArtifact> fetchedArtifacts = new HashSet<>();
            collectFetchedArtifacts(fetchResult.artifacts(), fetchedArtifacts);

            // Calculate checksums for all artifacts
            Map<String, String> checksums = new LinkedHashMap<>();
            checksums.put(jar.getName(), PluginValidator.calculateSha256(jar.toPath()));
            for (FetchedArtifact fetchedArtifact : fetchedArtifacts) {
                checksums.put(
                    fetchedArtifact.path().getFileName().toString(),
                    PluginValidator.calculateSha256(fetchedArtifact.path())
                );
            }
            getLog().info("Calculated SHA-256 checksums for " + checksums.size() + " artifacts");

            // Read existing manifest and create updated one with checksums
            Path manifestPath = outputDirectory.toPath().resolve(PluginFile.PLUGIN_MANIFEST_FILE);
            PluginManifest originalManifest;
            try (Reader reader = Files.newBufferedReader(manifestPath)) {
                originalManifest = PluginManifest.read(reader);
            }

            PluginManifest manifestWithChecksums = PluginManifest.builder()
                .group(originalManifest.group())
                .name(originalManifest.name())
                .version(originalManifest.version().toString())
                .displayName(originalManifest.displayName())
                .description(originalManifest.description())
                .application(originalManifest.application())
                .hostModule(originalManifest.hostModule())
                .url(originalManifest.url())
                .licenseName(originalManifest.licenseName())
                .licenseText(originalManifest.licenseText())
                .artifacts(originalManifest.artifacts() != null ? originalManifest.artifacts() : Map.of())
                .extensions(originalManifest.extensions() != null ? originalManifest.extensions() : Map.of())
                .extensionPoints(originalManifest.extensionPoints() != null ? originalManifest.extensionPoints() : List.of())
                .checksums(checksums)
                .build();

            // Write updated manifest directly to ZIP
            addManifestToZip(manifestWithChecksums, zipOut);

            // Add JAR and dependencies
            addZipEntry(jar.toPath(), zipOut);
            for (FetchedArtifact fetchedArtifact : fetchedArtifacts) {
                addZipEntry(fetchedArtifact.path(), zipOut);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating bundle file: " + bundleFile.getAbsolutePath(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new MojoExecutionException("SHA-256 algorithm not available", e);
        }

    }

    private void collectFetchedArtifacts(Stream<FetchedArtifact> artifacts, Set<FetchedArtifact> fetchedArtifacts) {
        for (var fetchedArtifact : artifacts.toList()) {
            if (fetchedArtifact.groupId().equals(hostGroupId) && fetchedArtifact.artifactId().equals(hostArtifactId)) {
               continue;
            }
            fetchedArtifacts.add(fetchedArtifact);
            collectFetchedArtifacts(fetchedArtifact.dependencies(), fetchedArtifacts);
        }
    }

    private void addZipEntry(Path path, ZipOutputStream zipOut) throws IOException {
        if (Files.notExists(path)) {
            throw new FileNotFoundException("File not found: " + path.toAbsolutePath());
        }
        getLog().info("Adding file to bundle: " + path.toAbsolutePath());
        File file = path.toFile();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            zipOut.putNextEntry(zipEntry);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fileInputStream.read(buffer)) > 0) {
                zipOut.write(buffer, 0, length);
            }
            zipOut.closeEntry();
        }
    }



    private void addManifestToZip(PluginManifest manifest, ZipOutputStream zipOut) throws IOException {
        ZipEntry zipEntry = new ZipEntry(PluginFile.PLUGIN_MANIFEST_FILE);
        zipOut.putNextEntry(zipEntry);
        StringWriter stringWriter = new StringWriter();
        manifest.write(stringWriter);
        zipOut.write(stringWriter.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zipOut.closeEntry();
    }

}
