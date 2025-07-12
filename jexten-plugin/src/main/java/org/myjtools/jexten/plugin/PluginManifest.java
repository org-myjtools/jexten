package org.myjtools.jexten.plugin;

import org.myjtools.jexten.Version;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class PluginManifest {

    public static PluginManifestBuilder builder() {
        return new PluginManifestBuilder();
    }


    private String group;
    private String name;
    private String version;
    private String application;
    private String hostModule;
    private String displayName;
    private String description;
    private String url;
    private String licenseName;
    private String licenseText;
    private Map<String,List<String>> artifacts;
    private Map<String,List<String>> extensions;
    private List<String> extensionPoints;


    private PluginManifest() {
        // Default constructor for deserialization
    }

    PluginManifest(PluginManifestBuilder builder) {
        this.group = builder.group();
        this.name = builder.name();
        this.version = builder.version();
        this.application = builder.application();
        this.displayName = builder.displayName();
        this.hostModule = builder.hostModule();
        this.description = builder.description();
        this.url = builder.url();
        this.licenseName = builder.licenseName();
        this.licenseText = builder.licenseText();
        this.artifacts = Map.copyOf(builder.artifacts());
        this.extensions = Map.copyOf(builder.extensions());
        this.extensionPoints = List.copyOf(builder.extensionPoints());
    }


    public static PluginManifest read(Reader reader) {
        Yaml yaml = new Yaml();
        yaml.setBeanAccess(BeanAccess.FIELD);
        return yaml.loadAs(reader, PluginManifest.class);
    }

    public void write(Writer writer) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        yaml.setBeanAccess(BeanAccess.FIELD);
        writer.write(yaml.dumpAsMap(this));
    }




    public PluginID id() {
        return new PluginID(group,name);
    }

    public Version version() {
        return Version.of(version);
    }

    public String group() {
        return group;
    }

    public String name() {
        return name;
    }

    public String application() {
        return application;
    }

    public String displayName() {
        return displayName;
    }

    public String hostModule() {
        return hostModule;
    }

    public String description() {
        return description;
    }

    public String url() {
        return url;
    }

    public String licenseName() {
        return licenseName;
    }

    public String licenseText() {
        return licenseText;
    }

    public Map<String, List<String>> artifacts() {
        return artifacts;
    }

    public Map<String, List<String>> extensions() {
        return extensions;
    }

    public List<String> extensionPoints() {
        return extensionPoints;
    }


}
