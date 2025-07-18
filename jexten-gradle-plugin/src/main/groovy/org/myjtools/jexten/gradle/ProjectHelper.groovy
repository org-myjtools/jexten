package org.myjtools.jexten.gradle

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedDependency
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.lang.module.ModuleFinder
import static org.myjtools.jexten.gradle.JextenGradlePlugin.*

class ProjectHelper {

    private static final Logger LOG = LoggerFactory.getLogger('org.myjtools.jexten.gradle')

    Project project
    String projectModule
    String applicationArtifact
    String parentArtifact

    Map<String, File> artifacts = new HashMap<>()
    Map<String, Set<String>> dependencies = new HashMap<>()


    ProjectHelper(Project project) {
        this.project = project
        this.projectModule = extractModule(new File(project.buildDir,'classes/java/main'))
        collectDependencies()
        int count = this.dependencies.values().stream().mapToInt { it.size()}.sum()
        int newCount
        while (true) {
            newCount = normalizeDependencies()
            if (newCount == count) break
            count = newCount
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Project dependencies are: ")
            LOG.info("-------------------------")
            dependencies.entrySet().forEach {
                LOG.info("$it.key :")
                it.value.forEach { LOG.info("  -- $it") }
            }
        }
    }


    File artifact(String coordinates) {
        if (!artifacts.containsKey(coordinates))
            throw new GradleException("$coordinates not present in project dependencies")
        return artifacts.get(coordinates)
    }


    Set<String> dependenciesExcluding(String...coordinates) {
        Set<String> output = new HashSet<>()
        dependencies.values().forEach { output.addAll(it) }
        for (String coor : coordinates) {
            if (coor == null) continue
            output.removeAll(dependencies.get(coor))
        }
        return output
    }


    Set<String> filteredDependencies() {
        return dependenciesExcluding(applicationArtifact,parentArtifact)
    }


    Set<File> filteredDependencyFiles() {
        Set<File> output = new HashSet<>()
        for (String key : filteredDependencies()) {
            output.add(artifacts.get(key))
        }
        return output
    }

    String moduleName(String coordinates) {
        return extractModule(artifact(coordinates))
    }


    private static String extractModule(File file) {
        def moduleReferences = ModuleFinder.of(file.toPath()).findAll()
        if (moduleReferences == null || moduleReferences.isEmpty()) {
            throw new GradleException("Cannot find a Java module in $file")
        }
        return moduleReferences.first().descriptor().name()
    }



    private collectDependencies() {
        def acceptedConfigurations = [
                'api',
                'implementation',
                CONFIGURATION_JEXTEN_APPLICATION,
                CONFIGURATION_JEXTEN_PARENT_ARTIFACT,
        ]
        for (def conf : project.configurations) {
            if (!acceptedConfigurations.contains(conf.name)) {
                continue
            }
            if (conf.name == CONFIGURATION_JEXTEN_APPLICATION) {
                if (conf.dependencies == null || conf.dependencies.isEmpty()) {
                    throw new GradleException("The plugin application must be declared as '$conf.name' in the dependencies section")
                }
                if (conf.dependencies.size() > 1) {
                    throw new GradleException("Only one dependency can be declared with '$conf.name'")
                }
            } else if (conf.name == CONFIGURATION_JEXTEN_PARENT_ARTIFACT) {
                if (conf.dependencies != null && conf.dependencies.size() > 1) {
                    throw new GradleException("Only one dependency can be declared with '$conf.name'")
                }
            }
            for (def dep : conf.dependencies) {
                String coordinates = "$dep.group:$dep.name:$dep.version"
                if (conf.name == CONFIGURATION_JEXTEN_APPLICATION) {
                    this.applicationArtifact = coordinates
                } else if (conf.name == CONFIGURATION_JEXTEN_PARENT_ARTIFACT) {
                    this.parentArtifact = coordinates
                }
                def detached = project.configurations.detachedConfiguration(dep)
                def resolved = detached.resolvedConfiguration
                this.artifacts.put(coordinates, resolved.resolvedArtifacts.first().file)
                this.dependencies.put(coordinates, transitiveDependencies(new HashSet<>(), resolved.firstLevelModuleDependencies))
                this.dependencies.get(coordinates).remove(coordinates)
            }
        }
    }


    private Set<String> transitiveDependencies(Set<String> output, Set<ResolvedDependency> dependencies) {
        for (def child : dependencies) {
            output.add("$child.moduleGroup:$child.moduleName:$child.moduleVersion")
            transitiveDependencies(output, child.children)
        }
        return output
    }


    private int normalizeDependencies() {
        for (def key : dependencies.keySet()) {
            for (def value : Set.copyOf(dependencies.get(key))) {
                if (dependencies.containsKey(value)) {
                    dependencies.get(key).addAll(dependencies.get(value))
                }
            }
        }
        return this.dependencies.values().stream().mapToInt { it.size()}.sum()
    }



    String metaInfExtensionFileContent() {
        def extensionFile = new File(project.buildDir,'classes/java/main/META-INF/extensions')
        if (extensionFile.exists()) {
            return extensionFile.text
        } else {
            return ''
        }
    }


    String metaInfExtensionPointFileContent() {
        def extensionPointFile = new File(project.buildDir,'classes/java/main/META-INF/extension-points')
        if (extensionPointFile.exists()) {
            return extensionPointFile.text
        } else {
            return ''
        }
    }


    String licenseFileContent() {
        def licenseFile = new File(project.projectDir,'LICENSE')
        if (licenseFile.exists()) {
            return licenseFile.text
        } else {
            return ''
        }
    }

}
