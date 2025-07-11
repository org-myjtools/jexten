module org.myjtools.jexten.plugin {
    exports org.myjtools.jexten.plugin;
    requires org.myjtools.jexten;
    requires org.yaml.snakeyaml;
    requires org.slf4j;
    requires java.net.http;
    opens org.myjtools.jexten.plugin to org.yaml.snakeyaml;
}