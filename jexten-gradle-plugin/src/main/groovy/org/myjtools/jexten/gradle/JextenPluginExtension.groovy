package org.myjtools.jexten.gradle


import org.gradle.api.provider.Property

interface JextenPluginExtension {
    Property<String> getDisplayName()
    Property<String> getDescription()
    Property<String> getLicenseName()
    Property<String> getURL()
}
