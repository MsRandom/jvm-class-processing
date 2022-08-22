package net.msrandom.extensions

import net.msrandom.extensions.annotations.ClassExtension
import net.msrandom.extensions.annotations.NonExtensionElement
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.*
import org.objectweb.asm.*
import org.objectweb.asm.tree.*
import java.io.File
import java.io.FileNotFoundException
import java.lang.invoke.MethodType.methodType
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

@CacheableTask
abstract class PostProcessClasses : DefaultTask() {
    abstract val extensionPackages: ListProperty<String>
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
            classesDirectory.finalizeValueOnRead()
            destinationDirectory.finalizeValueOnRead()
        }
    }


    private fun addMethod(baseNode: ClassNode, extensionNode: ClassNode, method: MethodNode) {
        if (method.name == "<clinit>") {
            val staticInitializer = baseNode.methods.firstOrNull { it.name == "<clinit>" }

            if (staticInitializer == null) {
                baseNode.methods.add(method)
            } else {
                val returnInstruction = staticInitializer.instructions.last { it.opcode == Opcodes.RETURN }

                repeat(method.instructions.size() - 1) {
                    val instruction = method.instructions.get(it)

                    staticInitializer.instructions.insertBefore(returnInstruction, instruction)
                }
            }
        } else {
            val index = baseNode.methods.indexOfFirst {
                it.name == method.name && methodType(baseNode, extensionNode, it) == methodType(baseNode, extensionNode, method)
            }

            if (index < 0) {
                baseNode.methods.add(method)
            } else {
                baseNode.methods[index] = method
            }
        }
    }

    private fun List<AnnotationNode>?.isIgnored(ignoreAnnotationDescriptor: String?) =
        ignoreAnnotationDescriptor != null && this?.any { it.desc == ignoreAnnotationDescriptor } == true

    private fun ClassNode.findAnnotation(annotationDescriptor: String) =
        invisibleAnnotations?.firstOrNull { it.desc == annotationDescriptor } ?: visibleAnnotations?.firstOrNull { it.desc == annotationDescriptor }

    private fun AnnotationNode.getValue() = values[values.indexOfFirst { it == "value" } + 1] as Type

    @TaskAction
    fun process() {
        val processed = hashSetOf<File>()

        val annotationDescriptor = Type.getDescriptor(ClassExtension::class.java)
        val ignoreAnnotationDescriptor = Type.getDescriptor(NonExtensionElement::class.java)

        val classes = classesDirectory.get()
        val destination = destinationDirectory.asFile.get()

        for (extensionPackage in extensionPackages.get()) {
            val packageDirectory = classes.dir(extensionPackage.asInternalName)

            if (!packageDirectory.asFile.exists()) {
                throw FileNotFoundException("Extension package $extensionPackage not found")
            }

            FILE_TREE@ for (file in packageDirectory.asFileTree) {
                val extensionNode = ClassNode()
                val reader = file.inputStream().use(::ClassReader)
                reader.accept(extensionNode, 0)

                val annotation = extensionNode.findAnnotation(annotationDescriptor)

                if (annotation == null) {
                    if (extensionNode.invisibleAnnotations.isIgnored(ignoreAnnotationDescriptor) || extensionNode.visibleAnnotations.isIgnored(ignoreAnnotationDescriptor)) {
                        continue
                    }

                    var currentNode = extensionNode
                    var index = currentNode.name.lastIndexOf('$')
                    val innerClassNames = mutableListOf<String>()
                    while (index > 0) {
                        val newName = currentNode.name.substring(0, index)
                        val newReader = classes.file("$newName.class").asFile.inputStream().use(::ClassReader)
                        innerClassNames.add(currentNode.name.substring(index + 1))
                        currentNode = ClassNode()
                        newReader.accept(currentNode, 0)

                        val newAnnotation = currentNode.findAnnotation(annotationDescriptor)
                        if (newAnnotation != null) {
                            val annotationValue = newAnnotation.getValue()
                            val name = extensionNode.name.replace(newName, annotationValue.internalName)
                            val output = destination.resolve("$name.class")

                            processed.add(classes.file("$name.class").asFile)
                            processed.add(file)

                            extensionNode.replaceClassReferences(newName, annotationValue.internalName)

                            var currentInnerName = newName
                            var currentBaseName = annotationValue.internalName
                            for (innerName in innerClassNames.asReversed()) {
                                currentInnerName += "$$innerName"
                                currentBaseName += "$$innerName"
                                extensionNode.replaceClassReferences(currentInnerName, currentBaseName)
                            }

                            val writer = ClassWriter(0)
                            extensionNode.accept(writer)

                            output.parentFile.toPath().createDirectories()
                            output.writeBytes(writer.toByteArray())

                            continue@FILE_TREE
                        } else {
                            index = currentNode.name.lastIndexOf('$')
                        }
                    }

                    throw UnsupportedOperationException("File $path in $extensionPackage does not contain the ${ClassExtension::class.qualifiedName} annotation")
                }

                val value = annotation.getValue()
                val baseRelativePath = "${value.internalName}.class"
                val basePath = classes.file(baseRelativePath).asFile

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

                for (innerClass in extensionNode.innerClasses) {
                    innerClass.name = innerClass.name.replace(extensionNode.name, baseNode.name)
                    innerClass.outerName = innerClass.outerName.replace(extensionNode.name, baseNode.name)
                }

                baseNode.innerClasses.addAll(extensionNode.innerClasses)

                for (field in extensionNode.fields) {
                    if (field.invisibleAnnotations.isIgnored(ignoreAnnotationDescriptor) || field.visibleAnnotations.isIgnored(ignoreAnnotationDescriptor)) continue

                    if (baseNode.fields.none { it.name == field.name }) {
                        baseNode.fields.add(field)
                    }
                }

                for (method in extensionNode.methods) {
                    if (method.invisibleAnnotations.isIgnored(ignoreAnnotationDescriptor) || method.visibleAnnotations.isIgnored(ignoreAnnotationDescriptor)) continue

                    addMethod(baseNode, extensionNode, method)
                }

                baseNode.replaceClassReferences(extensionNode.name, baseNode.name)

                processed.add(basePath)
                processed.add(file)

                val writer = ClassWriter(0)
                baseNode.accept(writer)

                outputPath.parentFile.toPath().createDirectories()
                outputPath.writeBytes(writer.toByteArray())
            }
        }

        copyRemainingFiles(classes, destination, processed)
    }

    private fun copyRemainingFiles(root: Directory, destination: File, processed: Set<File>) {
        for (file in root.asFileTree) {
            if (file in processed) continue

            val path = destination.resolve(file.relativeTo(root.asFile)).toPath()

            path.parent.createDirectories()
            file.toPath().copyTo(path, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
