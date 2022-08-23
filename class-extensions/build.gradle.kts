plugins {
    kotlin("jvm")
    `maven-publish`
    `java-gradle-plugin`
}

val pluginId = "jvm-class-extensions"

gradlePlugin {
    plugins {
        create("jvmClassExtensions") {
            id = pluginId
            implementationClass = "net.msrandom.extensions.JvmClassExtensionsPlugin"
        }
    }
}

group = "net.msrandom"
version = "1.1"

base {
    archivesName.set(pluginId)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(group = "org.ow2.asm", name = "asm-tree", version = "9.3")
    implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.12.0")
    implementation(projects.classExtensionAnnotations)

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("gradle") {
            groupId = base.archivesName.get()
            artifactId = "$groupId.gradle.plugin"

            from(components["java"])
        }
    }
}

tasks.test {
    dependsOn(tasks.pluginUnderTestMetadata)
    useJUnitPlatform()

    // TODO This might be unsafe, potentially find a way to do it with configurations instead?
    dependsOn(projects.classExtensionAnnotations.dependencyProject.tasks.publishToMavenLocal)
}
