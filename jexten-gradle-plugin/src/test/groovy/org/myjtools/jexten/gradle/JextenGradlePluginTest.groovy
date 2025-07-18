package org.myjtools.jexten.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JextenGradlePluginTest {

    @Test
    void pluginApplied() {

        var project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'org.myjtools.jexten.jexten-plugin'
        Assertions.assertNotNull(project.tasks.assemblePlugin)

    }
}
