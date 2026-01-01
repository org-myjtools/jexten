module org.myjtools.jexten.plugin.test {
    requires org.junit.jupiter.api;
    requires org.myjtools.jexten;
    requires org.myjtools.jexten.plugin;
    requires org.yaml.snakeyaml;
    requires org.assertj.core;

    exports org.myjtools.jexten.plugin.test to org.myjtools.jexten, org.junit.platform.commons;
    opens org.myjtools.jexten.plugin.test to org.junit.platform.commons;

}