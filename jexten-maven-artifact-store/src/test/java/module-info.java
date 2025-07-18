module org.myjtools.jexten.maven.artifact.store.test {
    requires org.junit.jupiter.api;
    requires org.assertj.core;
    requires org.myjtools.jexten.maven.artifact.store;
    requires org.myjtools.mavenfetcher;
    exports org.myjtools.jexten.maven.artifactstore.test to org.junit.platform.commons;
    opens org.myjtools.jexten.maven.artifactstore.test to org.junit.platform.commons;
}