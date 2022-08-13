package net.msrandom.postprocess

import org.gradle.api.Plugin
import org.gradle.api.plugins.PluginAware

class JvmPostProcessingPlugin<T : PluginAware> : Plugin<T> {
    override fun apply(target: T) = Unit
}
