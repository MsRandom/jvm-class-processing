plugins {
    kotlin("jvm") version "1.6.21"
    `java-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("jvmPostProcessing") {
            id = "jvm-post-processing"
            implementationClass = "net.msrandom.postprocess.JvmPostProcessingPlugin"
        }
    }
}

group = "net.msrandom"
version = "0.2"

group = rootProject.group
version = rootProject.version

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation(group = "org.ow2.asm", name = "asm-tree", version = "9.3")

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

tasks.test {
    dependsOn(tasks.pluginUnderTestMetadata)
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }

        create<MavenPublication>("gradle") {
            groupId = project.name
            artifactId = "${project.name}.gradle.plugin"

            from(components["java"])
        }
    }

    repositories {
        val credentials = (System.getenv("MAVEN_USERNAME") to System.getenv("MAVEN_PASSWORD")).takeIf { (username, password) -> username != null && password != null }

        credentials?.let { (mavenUsername, mavenPassword) ->
            maven("https://maven.msrandom.net/repository/root/").credentials {
                username = mavenUsername
                password = mavenPassword
            }
        }
    }
}
