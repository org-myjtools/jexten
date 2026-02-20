package org.myjtools.jexten.maven.artifactstore;


import org.myjtools.jexten.plugin.ArtifactStore;
import org.myjtools.mavenfetcher.MavenFetchRequest;
import org.myjtools.mavenfetcher.MavenFetcher;

import java.nio.file.Path;
import java.util.*;

/**
 * An implementation of ArtifactStore that retrieves artifacts from a Maven repository.
 * This class uses the MavenFetcher to fetch artifacts based on the provided properties.
 *
 * <p>To use this class, you can set properties that configure the MavenFetcher,
 * such as repository URLs, authentication details, and other settings.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 * {@code
 * MavenArtifactStore store = new MavenArtifactStore()
 *     .properties(new Properties());
 * Map<String, List<Path>> artifacts = store.retrieveArtifacts(artifactsMap);
 * }
 * </pre>
 */
public class MavenArtifactStore implements ArtifactStore {


    private Properties properties = new Properties();

    /**
     * Sets the properties for the Maven artifact store.
     * Check the documentation of MavenFetcher for the properties that can be set.
     * @param properties the properties to set
     * @return the current instance of MavenArtifactStore
     * @see org.myjtools.mavenfetcher.MavenFetcher
     */
    public MavenArtifactStore configure(Properties properties) {
        this.properties = properties;
        return this;
    }


    @Override
    public Map<String, List<Path>> retrieveArtifacts(Map<String, List<String>> artifacts) {
        MavenFetcher mavenFetcher = new MavenFetcher().config(properties);
        List<String> artifactRequest = getArtifactRequest(artifacts);
        var request = new MavenFetchRequest(artifactRequest).scopes("compile", "runtime");
        var result = new HashMap<String,List<Path>>();
        mavenFetcher.fetchArtifacts(request).allArtifacts().forEach(artifact -> {
            result.computeIfAbsent(artifact.groupId(), k -> new ArrayList<>()).add(artifact.path());
        });
        return result;
    }


    private static List<String> getArtifactRequest(Map<String, List<String>> artifacts) {
        List<String> artifactRequest = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : artifacts.entrySet()) {
            for (String artifact : entry.getValue()) {
                if (artifact.contains(":")) {
                    // Already in "artifactId:version" format
                    artifactRequest.add(entry.getKey() + ":" + artifact);
                } else {
                    // In "artifactId-version" format: find the version boundary
                    // (first hyphen followed by a digit) and convert to "artifactId:version"
                    int versionStart = findVersionBoundary(artifact);
                    if (versionStart != -1) {
                        String artifactId = artifact.substring(0, versionStart - 1);
                        String version = artifact.substring(versionStart);
                        artifactRequest.add(entry.getKey() + ":" + artifactId + ":" + version);
                    } else {
                        // No version found, use artifact name as-is
                        artifactRequest.add(entry.getKey() + ":" + artifact);
                    }
                }
            }
        }
        return artifactRequest;
    }


    private static int findVersionBoundary(String name) {
        for (int i = 0; i < name.length() - 1; i++) {
            if (name.charAt(i) == '-' && Character.isDigit(name.charAt(i + 1))) {
                return i + 1;
            }
        }
        return -1;
    }
}