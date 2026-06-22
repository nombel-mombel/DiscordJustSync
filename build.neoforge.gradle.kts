plugins {
	id("mod-platform")
	id("net.neoforged.moddev")
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

//tasks.jar {
//    dependsOn(tasks.shadowJar)
//}

stonecutter {
	val (version, loader) = current.project.split('-', limit = 2)
	properties.tags(version, loader)

	replacements.string(current.parsed >= "1.21.11") {
		replace("ResourceLocation", "Identifier")
		replace("location()", "identifier()")
	}
}

platform {
	loader = "neoforge"
	dependencies {
		required("minecraft") {
			forgeLikeVersionRange = prop("deps.minecraft")
		}
		required("neoforge") {
			forgeLikeVersionRange.set("[1,)")
		}
	}
}

neoForge {
	version = prop("deps.neoforge")
	accessTransformers.from(rootProject.file("src/main/resources/aw/${stonecutter.current.version}.cfg"))
	validateAccessTransformers = true

	if (hasProperty("deps.parchment")) parchment {
		val (mc, ver) = prop("deps.parchment").split(':')
		mappingsVersion = ver
		minecraftVersion = mc
	}

	runs {
		register("client") {
			client()
			gameDirectory = file("run/")
			ideName = "NeoForge Client (${stonecutter.current.version})"
			programArgument("--username=Dev")
		}
		register("server") {
			server()
			gameDirectory = file("run/")
			ideName = "NeoForge Server (${stonecutter.current.version})"
		}
	}

	mods {
		register(prop("mod.id")) {
			sourceSet(sourceSets["main"])
		}
	}
	sourceSets["main"].resources.srcDir("${rootDir}/versions/datagen/${sc.current.version.split("-")[0]}/src/main/generated")
}

repositories {
    mavenCentral()
    strictMaven("https://api.modrinth.com/maven", "maven.modrinth") { name = "Modrinth" }
//    strictMaven("https://maven.nucleoid.xyz/") {name = "Nucleoid"}
    strictMaven("https://repo.opencollab.dev/main/")
    strictMaven("https://jitpack.io")
    maven("https://maven.offsetmonkey538.top/releases") { name = "OffsetMonkey538" }
}

dependencies {
    embed("net.dv8tion:JDA:${prop("deps.jda")}") {
        exclude(module = "opus-java")
    }
    embed("club.minnced:discord-webhooks:${prop("deps.webhooks")}")
    embed("com.github.nombel-mombel:toml4j:${prop("deps.toml4j")}")

    // integrations
    compileOnly("net.luckperms:api:${prop("deps.luckperms-api")}")
    compileOnly("org.geysermc.floodgate:api:${prop("deps.floodgate-api")}")
    implementation("eu.pb4:placeholder-api-neoforge:${prop("deps.placeholder-api")}")
//    embed("eu.pb4:placeholder-api:${prop("deps.placeholder-api")}")
}

tasks.named("createMinecraftArtifacts") {
	dependsOn(tasks.named("stonecutterGenerate"))
}
