plugins {
	id("groovy-gradle-plugin")
	id("com.gradleup.shadow") version "8.3.6"
	id("com.github.breadmoirai.github-release") version "+"
}
base.archivesName.set(p("name"))
group = p("group")
version = p("version")
repositories {
	mavenCentral()
	mavenLocal()
	gradlePluginPortal()
	maven("https://maven.neoforged.net/releases")
}
@Suppress("HasPlatformType")
val shade by configurations.creating
configurations.implementation { extendsFrom(shade) }
java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
dependencies {
	compileOnly(gradleApi())
	shade("org.apache.commons:commons-lang3:3.18.0")
	shade("com.google.code.gson:gson:2.10")
	shade("org.ow2.asm:asm-util:9.7")
	shade("net.neoforged:accesstransformers:10.0.6") { exclude(group = "org.slf4j", module = "slf4j-api") }
	compileOnly("org.slf4j:slf4j-api:2.0.9")
}
tasks.shadowJar {
	archiveClassifier.set("")
	configurations = listOf(project.configurations.getByName("shade"))
	relocate("net.neoforged.jst", "io.github.forgestove.modaccessor.relocate.jst")
	relocate("net.neoforged.accesstransformers", "io.github.forgestove.modaccessor.relocate.accesstransformers")
	relocate("org.objectweb.asm", "io.github.forgestove.modaccessor.relocate.asm")
	relocate("org.apache.commons.lang3", "io.github.forgestove.modaccessor.relocate.commons.lang3")
}
gradlePlugin {
	website.set("https://github.com/ForgeStove/ModAccessor")
	vcsUrl.set("https://github.com/ForgeStove/ModAccessor")
	plugins {
		create(p("id")).apply {
			displayName = p("name")
			description = "A simple gradle plugin to solve the problem of accessing private fields and methods during compile time."
			id = "io.github.forgestove.modaccessor"
			tags.set(listOf("neoforge", "forge", "minecraft", "java"))
			implementationClass = "io.github.forgestove.modaccessor.ModAccessorPlugin"
		}
	}
}
githubRelease {
	token(System.getenv("GITHUB_TOKEN"))
	owner = p("auther")
	repo = p("name")
	tagName = p("version")
	releaseName = tagName
	generateReleaseNotes = true
	prerelease = false
	overwrite = true
}
fun p(key: String) = property(key).toString()
