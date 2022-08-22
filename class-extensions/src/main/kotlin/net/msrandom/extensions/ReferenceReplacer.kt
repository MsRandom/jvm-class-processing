package net.msrandom.extensions

import org.objectweb.asm.Handle
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureWriter
import org.objectweb.asm.tree.*

private fun Type.replaceExtensionReferences(extensionName: String, baseName: String): Type = if (sort == Type.ARRAY) {
    Type.getType("[${elementType.replaceExtensionReferences(extensionName, baseName).descriptor}")
} else if (sort == Type.OBJECT && internalName == extensionName) {
    Type.getObjectType(baseName)
} else {
    this
}

internal fun Type.replaceMethodDescriptor(extensionName: String, baseName: String): Type {
    val returnType = returnType.replaceExtensionReferences(extensionName, baseName)
    val argumentTypes = argumentTypes.map { it.replaceExtensionReferences(extensionName, baseName) }

    return Type.getMethodType(returnType, *argumentTypes.toTypedArray())
}

private fun List<Any>.replaceStackReferences(extensionName: String, baseName: String) = map {
    if (it is String) {
        Type.getObjectType(it).replaceExtensionReferences(extensionName, baseName).internalName
    } else {
        it
    }
}

private fun FieldInsnNode.replaceExtensionReferences(extensionName: String, baseName: String) {
    val internalExtensionName = "L$extensionName;"
    val internalBaseName = "L$baseName;"

    if (owner == extensionName) {
        owner = baseName
    }

    if (desc == internalExtensionName) {
        desc = internalBaseName
    }
}

private fun FrameNode.replaceExtensionReferences(extensionName: String, baseName: String) {
    local = local?.replaceStackReferences(extensionName, baseName)
    stack = stack?.replaceStackReferences(extensionName, baseName)
}

private fun InvokeDynamicInsnNode.replaceExtensionReferences(extensionName: String, baseName: String) {
    fun fixHandle(handle: Handle): Handle {
        val bsmDescriptor = Type.getType(handle.desc)

        val newOwner = if (handle.owner == extensionName) {
            baseName
        } else {
            handle.owner
        }

        val newDesc = if (bsmDescriptor.sort == Type.METHOD) {
            bsmDescriptor.replaceMethodDescriptor(extensionName, baseName).descriptor
        } else {
            bsmDescriptor.replaceExtensionReferences(extensionName, baseName).descriptor
        }

        return Handle(handle.tag, newOwner, handle.name, newDesc, handle.isInterface)
    }

    desc = Type.getMethodType(desc).replaceMethodDescriptor(extensionName, baseName).descriptor

    bsm = fixHandle(bsm)

    bsmArgs = bsmArgs.map {
        when (it) {
            is Type -> it.replaceExtensionReferences(extensionName, baseName)
            is Handle -> fixHandle(it)
            else -> it
        }
    }.toTypedArray()
}

private fun LdcInsnNode.replaceExtensionReferences(extensionName: String, baseName: String) {
    val cst = cst
    if (cst is Type) {
        this.cst = cst.replaceExtensionReferences(extensionName, baseName)
    }
}

private fun MethodInsnNode.replaceExtensionReferences(extensionName: String, baseName: String) {
    desc = Type.getMethodType(desc).replaceMethodDescriptor(extensionName, baseName).descriptor

    if (owner == extensionName) {
        owner = baseName
    }
}

private fun MultiANewArrayInsnNode.replaceExtensionReferences(extensionName: String, baseName: String) {
    desc = Type.getType(desc).replaceExtensionReferences(extensionName, baseName).descriptor
}

private fun TypeInsnNode.replaceExtensionReferences(extensionName: String, baseName: String) {
    desc = Type.getObjectType(desc).replaceExtensionReferences(extensionName, baseName).internalName
}

private fun AbstractInsnNode.replaceExtensionReferences(extensionName: String, baseName: String) {
    when (this) {
        is FieldInsnNode -> replaceExtensionReferences(extensionName, baseName)
        is FrameNode -> replaceExtensionReferences(extensionName, baseName)
        is InvokeDynamicInsnNode -> replaceExtensionReferences(extensionName, baseName)
        is LdcInsnNode -> replaceExtensionReferences(extensionName, baseName)
        is MethodInsnNode -> replaceExtensionReferences(extensionName, baseName)
        is MultiANewArrayInsnNode -> replaceExtensionReferences(extensionName, baseName)
        is TypeInsnNode -> replaceExtensionReferences(extensionName, baseName)
    }
}

private fun MethodNode.replaceExtensionReferences(extensionName: String, baseName: String) {
    desc = Type.getMethodType(desc).replaceMethodDescriptor(extensionName, baseName).descriptor

    for (localVariable in localVariables) {
        localVariable.desc = Type.getType(localVariable.desc).replaceExtensionReferences(extensionName, baseName).descriptor
    }

    for (instruction in instructions) {
        instruction.replaceExtensionReferences(extensionName, baseName)
    }
}

internal fun methodType(baseNode: ClassNode, extensionNode: ClassNode, method: MethodNode) =
    Type.getMethodType(method.desc).replaceMethodDescriptor(extensionNode.name, baseNode.name)

internal fun ClassNode.replaceClassReferences(extensionName: String, baseName: String) {
    name = name.replace(extensionName, baseName)
    signature = replaceSignature(signature, extensionName, baseName)
    interfaces = interfaces?.map { if (it == extensionName) baseName else it }

    if (superName == extensionName) {
        superName = baseName
    }

    if (outerClass == extensionName) {
        outerClass = baseName
    }

    if (nestHostClass == extensionName) {
        nestHostClass = baseName
    }

    outerMethodDesc = outerMethodDesc?.let { Type.getMethodType(it).descriptor }

    for (innerClass in innerClasses) {
        innerClass.name = innerClass.name.replace(extensionName, baseName)

        if (innerClass.outerName == extensionName) {
            innerClass.outerName = baseName
        }
    }

    for (field in fields) {
        field.signature = replaceSignature(field.signature, extensionName, baseName)
        field.desc = Type.getType(field.desc).descriptor
    }

    for (method in methods) {
        method.signature = replaceSignature(method.signature, extensionName, baseName)
        method.replaceExtensionReferences(extensionName, baseName)

        for (localVariable in method.localVariables) {
            localVariable.signature = replaceSignature(localVariable.signature, extensionName, baseName)
        }
    }
}

private fun replaceSignature(signature: String?, extensionName: String, baseName: String): String? {
    if (signature == null) return null

    val writer = ReplacingVisitor(extensionName, baseName)
    SignatureReader(signature).accept(writer)
    return writer.toString()
}

private class ReplacingVisitor(private val extensionName: String, private val baseName: String) : SignatureWriter() {
    override fun visitClassType(name: String) {
        if (name == extensionName) {
            super.visitClassType(baseName)
        } else {
            super.visitClassType(name)
        }
    }
}
