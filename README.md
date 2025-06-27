# ModAccessor

A simple gradle plugin to solve the problem of accessing private fields and methods during compile time.

**You should transform the classes in runtime by yourself.**

**Normally,if you pass AT files to forge/neoforge, it will apply to mod class automatically**

## Usage

[//]: # ([![ModAccessor]&#40;https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/dev/vfyjxf/modaccessor/dev.vfyjxf.modaccessor.gradle.plugin/maven-metadata.xml.svg?label=ModAccessor&#41;]&#40;https://plugins.gradle.org/plugin/dev.vfyjxf.modaccessor&#41;)

### Groovy DSL

```groovy
buildscript {
    repositories { maven { url 'https://jitpack.io' } }
    dependencies { classpath 'com.github.ForgeStove.ModAccessor:build:1.0.0' }
}
apply plugin: 'io.github.forgestove.modaccessor'
dependencies {
    accessCompileOnly("com.simibubi.create:create-1.21.1:6.0.4-61:slim")
}
```

### Kotlin DSL

```kotlin
buildscript {
	repositories { maven("https://jitpack.io") }
	dependencies { classpath("com.github.ForgeStove.ModAccessor:build:1.0.0") }
}
apply(plugin = "io.github.forgestove.modaccessor")
dependencies {
	add("accessCompileOnly", "com.simibubi.create:create-1.21.1:6.0.4-61:slim")
}
```

## Notice

The accessConfiguration isn't transitive, so you need to add the dependencies manually.
