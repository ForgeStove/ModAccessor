# ModAccessor

> **This project is forked from [vfyjxf/ModAccessor](https://github.com/vfyjxf/ModAccessor)**

A simple gradle plugin to solve the problem of accessing private fields and methods during compile time.

## Usage

### Groovy DSL

**build.gradle**

```groovy
plugins {
	id("io.github.forgestove.modaccessor") version "+"
}
dependencies {
    accessCompileOnly("com.simibubi.create:create-1.21.1:6.0.4-61:slim")
}
```

**settings.gradle**

```groovy
pluginManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
    resolutionStrategy.eachPlugin {
        if (requested.id.id != "io.github.forgestove.modaccessor") return
        useModule("com.github.ForgeStove.ModAccessor:build:${requested.version ?: "+"}")
    }
}
```

### Kotlin DSL

**build.gradle.kts**

```kotlin
plugins {
	id("io.github.forgestove.modaccessor") version "+"
}
dependencies {
	accessCompileOnly("com.simibubi.create:create-1.21.1:6.0.4-61:slim")
}
```

**settings.gradle.kts**

```kotlin
pluginManagement {
	repositories {
		maven("https://jitpack.io")
	}
	resolutionStrategy.eachPlugin {
		if(requested.id.id != "io.github.forgestove.modaccessor") return@eachPlugin
		useModule("com.github.ForgeStove.ModAccessor:build:${requested.version ?: "+"}")
	}
}
```

## Notice

* You should transform the classes in runtime by yourself.
* Normally,if you pass AT files to forge/neoforge, it will apply to mod class automatically.
* The accessConfiguration isn't transitive, so you need to add the dependencies manually.
* The plugin will automatically collect all `.cfg` files under `src/main/resources/META-INF` as default accessTransformerFiles.
