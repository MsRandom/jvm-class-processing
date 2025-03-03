# **THIS PROJECT IS ARCHIVED**
## This has been reworked into two different projects that now live in the [jvm-multiplatform](https://github.com/terrarium-earth/jvm-multiplatform) suite
<br/>
<br/>
<br/>

# jvm-class-extensions
Allows extending classes within the same compilation unit as a simple Gradle task, for example:

```java
package some.random.pkg;

// import ...;

public final class BaseClass {
    @ImplementedByExtension
    public BaseClass(int size) {
        throw NotImplementedException("BaseClass::new was not implemented properly.");
    }

    @ImplementedByExtension
    public int size() {
        throw NotImplementedException("BaseClass::size was not implemented properly.");
    }
}
```

```java
package some.random.pkg.extensions;

// import ...;

@ClassExtension(BaseClass.class)
class SomeExtension {
    private final int size;

    @ImplementsBaseElement
    public SomeExtension(int size) {
        this.size = size;
    }

    @ImplementsBaseElement
    public int size() {
        return size;
    }
}
```

The class file processing has to be implemented after the normal compilation of the source files, this can be done easily from within Gradle;

<details><summary>Kotlin DSL</summary>

```kotlin
classExtensions {
    registerForSourceSet(sourceSets.main.get(), "some.random.pkg.extensions")
    registerForSourceSet(sourceSets.main.get(), kotlin.sourceSets.main.get().kotlin, "some.random.pkg.extensions")
    registerForSourceSet(sourceSets.main.get(), sourceSets.main.get().groovy, "some.random.pkg.extensions")
}
```
</details>

<details><summary>Groovy DSL</summary>

```groovy
classExtensions {
    registerForSourceSet sourceSets.main, "some.random.pkg.extensions"
    registerForSourceSet sourceSets.main, kotlin.sourceSets.main.kotlin, "some.random.pkg.extensions"
    registerForSourceSet sourceSets.main, sourceSets.main.groovy, "some.random.pkg.extensions"
}
```
</details>
