package org.myjtools.jexten.plugin;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface ArtifactStore {

    /**
     * Retrieves artifacts based on the provided artifact identifiers.
     * @param artifacts A map where the keys are artifact groups and the values are lists of artifact identifiers.
     *                  Each artifact identifier can be in the format "artifactId:version" or just "artifactId" to retrieve the latest version.
     * @return A map where the keys are artifact groups and the values are lists of paths to the corresponding artifacts.
     */
    Map<String, List<Path>> retrieveArtifacts(Map<String, List<String>> artifacts);

}
