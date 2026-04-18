package org.myjtools.jexten.plugin;

import org.myjtools.jexten.plugin.internal.FileUtil;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

public class PluginRuntimeConfig {

    private Map<String, List<String>> artifacts;

    private PluginRuntimeConfig() {
        this.artifacts = new HashMap<>();
    }

    public static PluginRuntimeConfig empty() {
        return new PluginRuntimeConfig();
    }

    public static PluginRuntimeConfig read(Reader reader) {
        Yaml yaml = new Yaml();
        yaml.setBeanAccess(BeanAccess.FIELD);
        PluginRuntimeConfig config = yaml.loadAs(reader, PluginRuntimeConfig.class);
        if (config == null) {
            return empty();
        }
        if (config.artifacts == null) {
            config.artifacts = new HashMap<>();
        }
        return config;
    }

    public void write(Writer writer) throws IOException {
        Yaml yaml = FileUtil.yamlWriter();
        writer.write(yaml.dumpAsMap(this));
    }

    public Map<String, List<String>> artifacts() {
        return artifacts == null ? Map.of() : Collections.unmodifiableMap(artifacts);
    }

    public void addArtifact(String group, String artifact) {
        artifacts.computeIfAbsent(group, k -> new ArrayList<>());
        if (!artifacts.get(group).contains(artifact)) {
            artifacts.get(group).add(artifact);
        }
    }

    public boolean removeArtifact(String group, String artifact) {
        if (!artifacts.containsKey(group)) {
            return false;
        }
        boolean removed = artifacts.get(group).remove(artifact);
        if (artifacts.get(group).isEmpty()) {
            artifacts.remove(group);
        }
        return removed;
    }

    public boolean isEmpty() {
        return artifacts == null || artifacts.isEmpty();
    }
}