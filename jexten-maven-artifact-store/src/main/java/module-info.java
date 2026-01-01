/**
 * Maven artifact store implementation for the JExten plugin framework.
 * <p>
 * This module provides a {@code ArtifactStore} implementation that resolves
 * plugin artifacts from Maven repositories. It enables plugins to be specified
 * using standard Maven coordinates (groupId:artifactId:version) and handles:
 * <ul>
 *   <li>Artifact resolution from local and remote Maven repositories</li>
 *   <li>Transitive dependency resolution</li>
 *   <li>Artifact caching in the local Maven repository</li>
 * </ul>
 *
 * @see org.myjtools.jexten.plugin.ArtifactStore
 */
module org.myjtools.jexten.maven.artifact.store {

    // Public API: Contains MavenArtifactStore implementation of ArtifactStore
    exports org.myjtools.jexten.maven.artifactstore;

    // Plugin framework dependency for ArtifactStore interface
    requires org.myjtools.jexten.plugin;

    // Maven Fetcher library for artifact resolution and dependency management
    requires org.myjtools.mavenfetcher;
}
