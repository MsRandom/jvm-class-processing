plugins {
    kotlin("jvm") version "1.6.21"
    `java-gradle-plugin`
    `maven-publish`
}

group = "net.msrandom"
version = "0.1"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-gradle-plugin")
    apply(plugin = "maven-publish")

    group = rootProject.group
    version = rootProject.version

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
    }

    dependencies {
        implementation(gradleApi())

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
}
