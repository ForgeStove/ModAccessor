# ModAccessor

一个简单的Gradle Plugin,用于解决编译期无法访问私有类型/字段/方法的问题。

**注意:你需要自己处理运行时的access transform**

**通常来说，你可以直接向forge/neoforge提供AT文件,它会自动应用到mod类上**

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

### Notice

accessConfiguration不是transitive的，所以你需要手动添加依赖。
