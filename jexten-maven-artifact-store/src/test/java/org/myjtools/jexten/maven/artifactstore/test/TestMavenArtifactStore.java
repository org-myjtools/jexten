package org.myjtools.jexten.maven.artifactstore.test;

import org.junit.jupiter.api.Test;
import org.myjtools.jexten.maven.artifactstore.MavenArtifactStore;
import org.myjtools.mavenfetcher.MavenFetcherProperties;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class TestMavenArtifactStore {

    @Test
    void testMavenArtifactStore() {
        var store = new MavenArtifactStore();
        Properties properties = new Properties();
        properties.setProperty(MavenFetcherProperties.LOCAL_REPOSITORY, System.getProperty("user.home") + "/.m2/repository");
        store.configure(properties);
        Map<String, List<Path>> result = store.retrieveArtifacts(Map.of(
                "commons-io", List.of("commons-io-2.16.1"),
                "org.slf4j", List.of("slf4j-api-2.0.6"))
        );
        assertThat(result).hasEntrySatisfying(
            "commons-io:commons-io",
            paths -> assertThat(paths).hasSize(1)
                    .anySatisfy(path -> assertThat(path.getFileName().toString()).isEqualTo("commons-io-2.16.1.jar"))
        );
        assertThat(result).hasEntrySatisfying(
            "org.slf4j:slf4j-api",
            paths -> assertThat(paths).hasSize(1)
                    .anySatisfy(path -> assertThat(path.getFileName().toString()).isEqualTo("slf4j-api-2.0.6.jar"))
        );
    }

}
