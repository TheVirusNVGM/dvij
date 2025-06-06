@file:Suppress("UnstableApiUsage")

plugins {
	id("dev.architectury.loom")
	id("architectury-plugin")
}

val minecraft = stonecutter.current.version

version = "${prop("mod.version")}+$minecraft-playtesting"
base {
	archivesName.set("${prop("mod.id")}-common")
}

architectury.common(stonecutter.tree.branches.mapNotNull {
	if (stonecutter.current.project !in it) null
	else it.project.prop("loom.platform")
})

loom {
	silentMojangMappingsLicense()
	accessWidenerPath = rootProject.file("src/main/resources/${prop("mod.id")}.accesswidener")

	decompilers {
		get("vineflower").apply { // Adds names to lambdas - useful for mixins
			options.put("mark-corresponding-synthetics", "1")
		}
	}
}

repositories {
        maven("https://maven.terraformersmc.com/")
        maven("https://maven.isxander.dev/releases")
        maven("https://api.modrinth.com/maven")
}

dependencies {
	minecraft("com.mojang:minecraft:$minecraft")
        mappings(loom.layered {
                officialMojangMappings()
        })
        modImplementation("net.fabricmc:fabric-loader:${versionProp("fabric_loader")}")

        // Mod implementations
}

tasks.processResources {
	applyProperties(project, listOf("${prop("mod.id")}-common.mixin.json"))
}

java {
	withSourcesJar()
	val java = if (stonecutter.eval(minecraft, ">=1.20.5"))
		JavaVersion.VERSION_21 else JavaVersion.VERSION_17
	targetCompatibility = java
	sourceCompatibility = java
}

tasks.build {
	group = "versioned"
	description = "Must run through 'chiseledBuild'"
}