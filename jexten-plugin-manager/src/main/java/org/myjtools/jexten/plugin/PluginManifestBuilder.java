package org.myjtools.jexten.plugin;

import java.util.*;

public class PluginManifestBuilder {

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
    private Map<String, List<String>> artifacts = new HashMap<>();
    private Map<String, List<String>> extensions = new HashMap<>();
    private List<String> extensionPoints = new ArrayList<>();
    private Map<String, String> checksums = new HashMap<>();

    public PluginManifestBuilder group(String group) {
        this.group = group;
        return this;
    }

    public PluginManifestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public PluginManifestBuilder version(String version) {
        this.version = version;
        return this;
    }

    public PluginManifestBuilder application(String application) {
        this.application = application;
        return this;
    }

    public PluginManifestBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public PluginManifestBuilder hostModule(String hostModule) {
        this.hostModule = hostModule;
        return this;
    }

    public PluginManifestBuilder description(String description) {
        this.description = description;
        return this;
    }

    public PluginManifestBuilder url(String url) {
        this.url = url;
        return this;
    }

    public PluginManifestBuilder licenseName(String licenseName) {
        this.licenseName = licenseName;
        return this;
    }

    public PluginManifestBuilder licenseText(String licenseText) {
        this.licenseText = licenseText;
        return this;
    }

    public PluginManifestBuilder artifacts(Map<String, List<String>> artifacts) {
        this.artifacts = artifacts;
        return this;
    }

    public PluginManifestBuilder extensions(Map<String, List<String>> extensions) {
        this.extensions = extensions;
        return this;
    }

    public PluginManifestBuilder extensionPoints(List<String> extensionPoints) {
        this.extensionPoints = extensionPoints;
        return this;
    }

    public PluginManifestBuilder checksums(Map<String, String> checksums) {
        this.checksums = checksums;
        return this;
    }

    public PluginManifest build() {
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(version, "version must not be null");
        return new PluginManifest(this);
    }


    public String group() {
        return group;
    }

    public String name() {
        return name;
    }

    public String version() {
        return version;
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

    public Map<String, String> checksums() {
        return checksums;
    }
}