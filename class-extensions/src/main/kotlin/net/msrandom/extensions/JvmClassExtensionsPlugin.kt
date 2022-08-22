package net.msrandom.extensions

import org.gradle.api.Plugin
import org.gradle.api.Project

class JvmClassExtensionsPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("classExtensions", ClassExtensionsExtension::class.java, target)
    }
}
