plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.fabric.loom)
    `maven-publish`
}

group = "xyz.crunchmunch.mods"
version = "1.0.0"

base {
    archivesName.set("gamemaster")
}

loom {
    accessWidenerPath = file("src/main/resources/gamemaster.accesswidener")
}

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://maven.nucleoid.xyz/releases")
    maven("https://jitpack.io")
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)

    implementation(libs.fabric.kotlin)
    api(libs.brigadier.kotlin)

    api(libs.spectatorapi)
}

tasks {
    processResources {
        filteringCharset = "UTF-8" // We want UTF-8 for everything
        var props = mapOf(
            "version" to project.version,
            "loader_version" to libs.versions.fabric.loader.get(),
            "fabric_version" to libs.versions.fabric.api.get(),
            "minecraft_version" to libs.versions.minecraft.get(),
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") {
            expand(props)
        }
    }
}
