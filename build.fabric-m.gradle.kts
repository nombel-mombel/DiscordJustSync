import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.utils.extendsFrom

plugins {
	id("mod-platform")
	id("net.fabricmc.fabric-loom")
    id("com.gradleup.shadow") version "9+"
}

val embed by configurations.creating
configurations {
    implementation {
        extendsFrom(embed)
    }
}
tasks.shadowJar {
    configurations = listOf(embed)
}

tasks.jar {
    dependsOn(tasks.shadowJar)
}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)

	replacements.string(current.parsed >= "1.21.11") {
		replace("ResourceLocation", "Identifier")
		replace("location()", "identifier()")
	}
	replacements.string(current.parsed >= "26.1.2") {
		replace("FabricDataOutput", "FabricPackOutput")
	}
}

platform {
	loader = "fabric-m"
	dependencies {
		required("minecraft") {
			fabricLikeVersionRange = prop("deps.minecraft")
		}
		required("fabric-api") {
			slug("fabric-api")
			fabricLikeVersionRange = ">=${prop("deps.fabric-api")}"
		}
		required("fabricloader") {
			fabricLikeVersionRange = ">=${prop("deps.fabric-loader")}"
		}
        required("placeholder-api") {
            fabricLikeVersionRange = ">=2.5.1"
        }
	}
}

loom {
	accessWidenerPath = rootProject.file("src/main/resources/aw/${stonecutter.current.version}.accesswidener")
	runs.named("client") {
		client()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "client"
		programArgs("--username=Dev")
		configName = "Fabric Client"
	}
	runs.named("server") {
		server()
		ideConfigGenerated(true)
		runDir = "run/"
		environment = "server"
		configName = "Fabric Server"
	}
}

fabricApi {
	configureDataGeneration {
		outputDirectory = file("${rootDir}/versions/datagen/${sc.current.version.split("-")[0]}/src/main/generated")
		client = true
	}
}

repositories {
	mavenCentral()
//	strictMaven("https://maven.terraformersmc.com/", "com.terraformersmc") { name = "TerraformersMC" }
	strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
    strictMaven("https://maven.nucleoid.xyz/") {name = "Nucleoid"}
    strictMaven("https://repo.opencollab.dev/main/")
    strictMaven("https://jitpack.io")
}

dependencies {
	minecraft("com.mojang:minecraft:${prop("deps.minecraft")}")
	implementation("net.fabricmc:fabric-loader:${prop("deps.fabric-loader")}") // fabric only
	implementation("net.fabricmc.fabric-api:fabric-api:${prop("deps.fabric-api")}") // fabric only
    embed("me.lucko:fabric-permissions-api:${prop("deps.farbic-permission-api")}") // fabric only
    embed("net.dv8tion:JDA:${prop("deps.jda")}") {
        exclude(module = "opus-java")
    }
    embed("club.minnced:discord-webhooks:${prop("deps.webhooks")}")
    embed("com.github.nombel-mombel:toml4j:${prop("deps.toml4j")}")

    // integrations
    compileOnly("net.luckperms:api:${prop("deps.luckperms-api")}")
    compileOnly("org.geysermc.floodgate:api:${prop("deps.floodgate-api")}")
    compileOnly("maven.modrinth:vanish:${prop("deps.vanish")}") // fabric only

    implementation("eu.pb4:placeholder-api:${prop("deps.placeholder-api")}")
}
