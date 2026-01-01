package org.myjtools.jexten.plugin;

import org.myjtools.jexten.Version;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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


    /**
     * Reads and validates a plugin manifest from a YAML source.
     *
     * @param reader the reader containing the YAML manifest content
     * @return a validated PluginManifest instance
     * @throws InvalidManifestException if the manifest fails validation
     */
    public static PluginManifest read(Reader reader) {
        Yaml yaml = new Yaml();
        yaml.setBeanAccess(BeanAccess.FIELD);
        PluginManifest manifest = yaml.loadAs(reader, PluginManifest.class);
        manifest.validate();
        return manifest;
    }


    /**
     * Validates this manifest, checking for required fields and correct formats.
     *
     * @throws InvalidManifestException if validation fails
     */
    public void validate() {
        List<String> errors = new ArrayList<>();

        // Required fields
        if (isBlank(group)) {
            errors.add("'group' is required");
        }
        if (isBlank(name)) {
            errors.add("'name' is required");
        }
        if (isBlank(version)) {
            errors.add("'version' is required");
        } else if (!Version.validate(version)) {
            errors.add("'version' must be a valid semantic version (e.g., '1.0.0'), got: " + version);
        }
        if (isBlank(hostModule)) {
            errors.add("'hostModule' is required");
        }

        // Validate artifacts map structure
        if (artifacts != null) {
            for (var entry : artifacts.entrySet()) {
                if (isBlank(entry.getKey())) {
                    errors.add("artifact key cannot be blank");
                }
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    errors.add("artifact '" + entry.getKey() + "' must have at least one dependency");
                }
            }
        }

        // Validate extensions map structure
        if (extensions != null) {
            for (var entry : extensions.entrySet()) {
                if (isBlank(entry.getKey())) {
                    errors.add("extension point key cannot be blank");
                }
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    errors.add("extension point '" + entry.getKey() + "' must have at least one implementation");
                }
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidManifestException(errors);
        }
    }


    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        PluginManifest manifest = (PluginManifest) o;
        return Objects.equals(group, manifest.group) && Objects.equals(name, manifest.name) && Objects.equals(version, manifest.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, name, version);
    }
}
