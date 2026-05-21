import org.apache.commons.lang3.SystemUtils
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files

plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

//Constants:

val baseGroup: String by project
val mcVersion: String by project
val version: String by project
val mixinGroup = "$baseGroup.mixin"
val modid: String by project

// Toolchains:
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

// Minecraft configuration:
loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) {
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.$modid.json")
    }
    mixin {
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}

sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}

// Dependencies:

repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}

val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")

    compileOnly(files("C:/Users/blood/.lunarclient/offline/multiver/overrides/ReplayMod-v1_8-2.6.14.jar"))

    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
}

// Tasks:

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}

tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set(modid)
    manifest.attributes.run {
        this["MixinConfigs"] = "mixins.$modid.json"
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modid", modid)
    inputs.property("basePackage", baseGroup)

    filesMatching("mixins.$modid.json") {
        expand(inputs.properties)
    }
}


tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
}

tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("non-obfuscated-with-deps")
    configurations = listOf(shadowImpl)
    doLast {
        configurations.forEach {
            println("Copying dependencies into mod: ${it.files}")
        }
    }

    // Relocate mixin classes into replaymod's mixin package so the framework's package prefix prepend produces the correct FQCN at runtime.
    relocate("$baseGroup.mixin", "com.replaymod.replay.mixin.$modid")

    fun relocate(name: String) = relocate(name, "$baseGroup.deps.$name")
}

// Post-process the shadow JAR to update mixin config JSON package fields
// to match the shadow-relocated class paths. Shadow only relocates .class
// files, not string contents inside JSON resources.
val fixMixinConfigs by tasks.registering {
    dependsOn(tasks.shadowJar)
    val shadowOut = tasks.shadowJar.get().archiveFile
    inputs.file(shadowOut)

    doLast {
        val jar = shadowOut.get().asFile
        val relocatedPkg = "com.replaymod.replay.mixin.$modid"
        val configName = "mixins.$modid.json"

        val uri = URI.create("jar:" + jar.toURI())
        val fs = FileSystems.newFileSystem(uri, emptyMap<String, Any>())
        fs.use { zipFs ->
            val entry = zipFs.getPath(configName)
            if (Files.exists(entry)) {
                val bytes = Files.readAllBytes(entry)
                val text = String(bytes, Charsets.UTF_8)
                val updated = text.replace(
                    "\"package\": \"$baseGroup.mixin\"",
                    "\"package\": \"$relocatedPkg\""
                )
                if (updated != text) {
                    Files.write(entry, updated.toByteArray(Charsets.UTF_8))
                    println("fixMixinConfigs: updated package -> $relocatedPkg")
                } else {
                    println("fixMixinConfigs: no match found in $configName (already correct?)")
                }
            } else {
                println("fixMixinConfigs: $configName not found in JAR")
            }
        }
    }
}

// Skip Loom's remapJar — Ichor's runtime uses MCP names (getMinecraft, not
// func_71410_x), so remapping to SRG would break all direct MC references.
// The shadow JAR (with fixMixinConfigs applied) is our final artifact.
tasks.named("remapJar") { enabled = false }

// Rename the shadow JAR to modid-version.coffeeclient.jar
val renameToCoffeeJar by tasks.registering(Copy::class) {
    dependsOn(fixMixinConfigs)
    from(tasks.shadowJar.get().archiveFile)
    into(layout.buildDirectory.dir("libs"))
    rename { "${modid}-${version}.coffeeclient.jar" }
}

tasks.assemble.get().dependsOn(renameToCoffeeJar)

// Deploy: copy the .coffeeclient.jar to coffeeloader/mods/
tasks.register<Copy>("idep") {
    dependsOn(renameToCoffeeJar)
    from(layout.buildDirectory.dir("libs")) {
        include("*.coffeeclient.jar")
    }
    into(file("F:\\CoffeeClient\\coffeemods"))
    doFirst {
        file("F:\\CoffeeClient\\coffeemods").mkdirs()
    }
    doLast {
        println("Deployed ${modid}-${version}.coffeeclient.jar -> F:\\CoffeeClient\\coffeemods\\")
    }
}

