package net.msrandom.postprocess

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.function.Predicate
import kotlin.io.path.*

@CacheableTask
abstract class PostProcessClasses : DefaultTask() {
    abstract val extensionPackages: ListProperty<String>
        @Input get

    abstract val annotationType: Property<String>
        @Input get

    abstract val classesDirectory: DirectoryProperty
        @InputDirectory
        @PathSensitive(PathSensitivity.RELATIVE)
        get

    abstract val destinationDirectory: DirectoryProperty
        @OutputDirectory get

    private val String.asInternalName
        get() = replace(".", "/")

    init {
        apply {
            extensionPackages.finalizeValueOnRead()
            annotationType.finalizeValueOnRead()
            classesDirectory.finalizeValueOnRead()
        }
    }

    private fun addMethod(node: ClassNode, method: MethodNode) {
        if (method.name == "<cinit>") {
            val staticInitializer = node.methods.firstOrNull { it.name == "<cinit>" }

            if (staticInitializer == null) {
                node.methods.add(method)
            } else {
                val beforeReturn = staticInitializer.instructions.get(staticInitializer.instructions.size() - 1)

                repeat(method.instructions.size() - 1) {
                    val instruction = method.instructions.get(it)

                    staticInitializer.instructions.insert(beforeReturn, instruction)
                }
            }
        } else {
            val index = node.methods.indexOfFirst { it.name == method.name && it.desc == method.desc }

            if (index < 0) {
                if (method.name != "<init>") {
                    node.methods.add(method)
                }
            } else {
                node.methods[index] = method
            }
        }
    }

    @TaskAction
    fun process() {
        val processed = hashSetOf<Path>()

        val annotationDescriptor = "L${annotationType.get().asInternalName};"

        val classes = classesDirectory.asFile.get().toPath()
        val destination = destinationDirectory.asFile.get().toPath()

        for (extensionPackage in extensionPackages.get()) {
            val packagePath = classes.resolve(extensionPackage.asInternalName)

            if (packagePath.notExists()) {
                throw FileNotFoundException("Extension package $extensionPackage not found")
            }

            Files.walk(packagePath).use { stream ->
                for (path in stream.filter(Path::isRegularFile)) {
                    val extensionNode = ClassNode()
                    val reader = path.inputStream().use(::ClassReader)
                    reader.accept(extensionNode, 0)

                    if (extensionNode.access and Opcodes.ACC_ANNOTATION != 0 || extensionNode.sourceFile == "package-info.java") {
                        continue
                    }

                    val annotation = extensionNode.invisibleAnnotations?.firstOrNull { it.desc == annotationDescriptor }
                        ?: throw UnsupportedOperationException("File $path in $extensionPackage does not contain the ${this.annotationType.get()} annotation")

                    val value = annotation.values[annotation.values.indexOfFirst { it == "value" } + 1] as Type
                    val baseRelativePath = "${value.internalName}.class"
                    val basePath = classes.resolve(baseRelativePath)

                    if (!basePath.exists()) {
                        throw FileNotFoundException("Type $value not found")
                    }

                    val outputPath = destination.resolve(baseRelativePath)
                    val baseNode = ClassNode()
                    val baseReader = basePath.inputStream().use(::ClassReader)

                    baseReader.accept(baseNode, 0)

                    if (baseNode.interfaces == null) {
                        baseNode.interfaces = extensionNode.interfaces
                    } else if (extensionNode.interfaces != null) {
                        baseNode.interfaces = (baseNode.interfaces + extensionNode.interfaces).distinct()
                    }

                    for (field in extensionNode.fields) {
                        if (baseNode.fields.none { it.name == field.name }) {
                            baseNode.fields.add(field)
                        }
                    }

                    for (method in extensionNode.methods) {
                        addMethod(baseNode, method)
                    }

                    for (method in baseNode.methods) {
                        method.replaceExtensionReferences(extensionNode.name, baseNode.name)
                    }

                    processed.add(basePath)
                    processed.add(path)

                    val writer = ClassWriter(0)
                    baseNode.accept(writer)

                    outputPath.parent.createDirectories()
                    outputPath.writeBytes(writer.toByteArray())
                }
            }
        }

        copyRemainingFiles(classes, destination, processed)
    }

    private fun copyRemainingFiles(root: Path, destination: Path, processed: Set<Path>) {
        Files.walk(root).filter(Path::isRegularFile).filter(Predicate<Path>(processed::contains).negate()).use {
            for (path in it) {
                val newPath = destination.resolve(root.relativize(path).toString())

                newPath.parent.createDirectories()
                path.copyTo(newPath, StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
