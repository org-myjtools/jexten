package org.myjtools.jexten.plugin;

public record PluginID(
    String group,
    String name
) {

    @Override
    public String toString() {
        return group+":"+name;
    }
}
