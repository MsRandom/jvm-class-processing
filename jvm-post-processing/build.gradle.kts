gradlePlugin {
    plugins {
        create("jvmPostProcessing") {
            id = "jvm-post-processing"
            implementationClass = "net.msrandom.postprocess.JvmPostProcessingPlugin"
        }
    }
}

dependencies {
    implementation(group = "org.ow2.asm", name = "asm-tree", version = "9.3")
}
