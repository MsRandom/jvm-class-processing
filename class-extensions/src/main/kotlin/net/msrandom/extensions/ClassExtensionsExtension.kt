package net.msrandom.extensions

import net.msrandom.extensions.annotations.ClassExtension
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.configurationcache.extensions.capitalized

open class ClassExtensionsExtension(private val project: Project) {
    /**
     * Registers the needed task and reorders source set directories to allow the classes to be processed before the source set's classes task
     *
     * @param sourceSet The source set to add a [PostProcessClasses] to
     * @param packages The packages which contain the extension classes(the classes marked with [ClassExtension]])
     */
    @Suppress("unused")
    fun registerForSourceSet(sourceSet: SourceSet, vararg packages: String) = registerForSourceSet(sourceSet, sourceSet.java, *packages)

    /**
     * Registers the needed task and reorders source set directories to allow the classes to be processed before the source set's classes task
     *
     * @param sourceSet The source set to add a [PostProcessClasses] to
     * @param sourceDirectorySet The directory set to be used, is [sourceSet].java by default. Can be used with other JVM languages as long as they have a [SourceDirectorySet]
     * @param packages The packages which contain the extension classes(the classes marked with [ClassExtension]])
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun registerForSourceSet(sourceSet: SourceSet, sourceDirectorySet: SourceDirectorySet, vararg packages: String) {
        val sourceSetPart = if (sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME) "" else sourceSet.name.capitalized()

        val task = project.tasks.maybeCreate("process${sourceSetPart}${sourceDirectorySet.name.capitalized()}Classes", PostProcessClasses::class.java)
        task.extensionPackages.addAll(*packages)

        project.tasks.withType(AbstractCompile::class.java).getByName(sourceSet.getCompileTaskName(sourceDirectorySet.name)) {
            it.destinationDirectory.set(sourceDirectorySet.destinationDirectory.get())
            task.classesDirectory.set(it.destinationDirectory)
        }

        sourceDirectorySet.destinationDirectory.set(project.layout.buildDirectory.dir(PROCESSED_CLASSES).map { it.dir(sourceDirectorySet.name).dir(sourceSet.name) })
        task.destinationDirectory.set(sourceDirectorySet.destinationDirectory)

        sourceSet.compiledBy(task)

        project.tasks.named(sourceSet.classesTaskName) {
            it.dependsOn(task)
        }

        project.dependencies.add(sourceSet.compileOnlyConfigurationName, "net.msrandom:class-extension-annotations:1.0")
    }

    companion object {
        const val PROCESSED_CLASSES = "processedClasses"
    }
}
