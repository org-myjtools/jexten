package org.myjtools.jexten.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.myjtools.mavenfetcher.FetchedArtifact;
import org.myjtools.mavenfetcher.MavenFetchRequest;
import org.myjtools.mavenfetcher.MavenFetchResult;
import org.myjtools.mavenfetcher.MavenFetcher;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
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
            addZipEntry(outputDirectory.toPath().resolve("plugin.yaml"), zipOut);
            File jar = new File(buildDirectory, project.getArtifactId() + "-" + project.getVersion() + ".jar");
            addZipEntry(jar.toPath(), zipOut);
            Set<FetchedArtifact> fetchedArtifacts = new HashSet<>();
            collectFetchedArtifacts(fetchResult.artifacts(), fetchedArtifacts);
            for (FetchedArtifact fetchedArtifact : fetchedArtifacts) {
                addZipEntry(fetchedArtifact.path(), zipOut);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error creating bundle file: " + bundleFile.getAbsolutePath(), e);
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





}
