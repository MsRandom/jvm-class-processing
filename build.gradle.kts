plugins {
    kotlin("jvm") version "1.6.21" apply false
    java
    `maven-publish`
}

subprojects {
    apply<JavaPlugin>()
    apply<MavenPublishPlugin>()

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }

        withSourcesJar()
        withJavadocJar()
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
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
