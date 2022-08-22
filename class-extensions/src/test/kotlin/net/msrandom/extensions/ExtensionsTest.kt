package net.msrandom.extensions

import org.gradle.api.tasks.SourceSet
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.platform.commons.annotation.Testable
import java.io.File
import java.nio.file.Path
import java.security.SecureClassLoader
import java.util.function.IntSupplier
import java.util.function.IntToLongFunction
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testable
class ExtensionsTest {
    @Test
    fun `Extend class and test resulting functions`() {
        val baseClassName = "net.msrandom.extensions.test.BaseClass"
        val projectDir = File("extensions-test")

        val processedClassFile = projectDir.toPath()
            .resolve("build")
            .resolve(ClassExtensionsExtension.PROCESSED_CLASSES)
            .resolve("java")
            .resolve(SourceSet.MAIN_SOURCE_SET_NAME)
            .resolve("${baseClassName.replace('.', File.separatorChar)}.class")

        GradleRunner.create()
            .withPluginClasspath()
            .withProjectDir(projectDir)
            .withArguments("classes", "-s")
            .forwardOutput()
            .withDebug(false)
            .build()

        assertTrue(processedClassFile.exists(), "BaseClass was not processed properly or outputted into the proper directory.")

        val x = 3
        val y = 4
        val z = 5

        val loadedClass = ByteCodeClassLoader().loadClass(baseClassName, processedClassFile)
        val area = loadedClass.getDeclaredMethod("area")
        val areaFunction = loadedClass.getDeclaredMethod("areaFunction")
        val volumeByDepth = loadedClass.getDeclaredMethod("volumeByDepth")
        val factory = loadedClass.getDeclaredMethod("of", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
        val instance = factory(null, x, y)

        assertEquals(x * y, area(instance), "BaseClass::area returned an unexpected result, meaning something went wrong during processing.")
        assertEquals(x * y, (areaFunction(instance) as IntSupplier).asInt, "BaseClass::areaFunction returned an unexpected result, meaning something went wrong during processing.")
        assertEquals(x * y * z.toLong(), (volumeByDepth(instance) as IntToLongFunction).applyAsLong(z), "BaseClass::volumeByDepth returned an unexpected result, meaning something went wrong during processing.")
    }

    private class ByteCodeClassLoader : SecureClassLoader() {
        fun loadClass(name: String, path: Path): Class<*> {
            val bytes = path.readBytes()
            return super.defineClass(name, bytes, 0, bytes.size)
        }
    }
}
