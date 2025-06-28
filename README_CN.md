# ModAccessor

**语言选择**: [English](README.md) | [中文](README_CN.md)

> **本项目复刻自 [vfyjxf/ModAccessor](https://github.com/vfyjxf/ModAccessor)**

一个简单的Gradle Plugin,用于解决编译期无法访问私有类型/字段/方法的问题。

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

* 你需要自己处理运行时的 AccessTransform
* 通常来说，你可以直接向forge/neoforge提供AT文件,它会自动应用到mod类上
* accessConfiguration是不可传递依赖的，所以你需要手动添加依赖
* 插件会自动收集 `src/main/resources/META-INF` 目录下的所有 `.cfg` 文件作为默认的 accessTransformerFiles
