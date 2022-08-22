plugins {
    java
    id("jvm-class-extensions")
}

classExtensions {
    registerForSourceSet(sourceSets.main.get(), "net.msrandom.extensions.test.extensions")
}

repositories {
    mavenLocal()
}
