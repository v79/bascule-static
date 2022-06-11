import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(from = "gradle.properties")
val kotlin_version: String by extra
val bascule_lib_version: String by extra
val handlebars_version: String by extra
val slf4j_version: String by extra
val flexmark_version: String by extra
val spek_version: String by extra
val mockk_version: String by extra
val picocli_version: String by extra
val jansi_version: String by extra
val snakeyaml_version: String by extra
val koin_version: String by extra
val serialization_version: String by extra
val coroutines_version: String by extra

buildscript {
}
plugins {
	kotlin("jvm") version "1.6.21"
	`maven-publish`
	id("com.github.johnrengelman.shadow") version "6.1.0"
	kotlin("plugin.serialization") version "1.6.21"
}

group = "org.liamjd"
version = "0.0.28"

repositories {
	mavenCentral()
	jcenter()
	mavenLocal()
}

dependencies {
	// stdlib
	implementation(kotlin("stdlib"))
	// reflection
	implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
	// coroutines
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version")

	// bascule library
	implementation("org.liamjd.bascule-lib:bascule-lib:$bascule_lib_version")

	// handlebars templating
	implementation("com.github.jknack:handlebars:$handlebars_version")

	// markdown - probably want to be more selective with this!
	implementation("com.vladsch.flexmark:flexmark-all:$flexmark_version")
	implementation("com.vladsch.flexmark:flexmark-ext-tables:$flexmark_version")

	// sl4j logging
	implementation("org.slf4j:slf4j-simple:$slf4j_version")
	implementation("io.github.microutils:kotlin-logging:1.6.26")

	// command line parsing
	implementation("info.picocli:picocli:$picocli_version")
	implementation("org.fusesource.jansi:jansi:$jansi_version")

	// for console progress bar?
	implementation("com.vdurmont:etaprinter:1.0.0")

	// kotlinx serialization, for document caching
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
	testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spek_version")
	testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spek_version")
	testImplementation("io.mockk:mockk:$mockk_version")
	testImplementation(kotlin("test"))
}

tasks.test {
	useJUnitPlatform {
		includeEngines("spek2")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "11"
}


tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
	archiveClassifier.set("")
	manifest {
		attributes(
			mapOf(
				"Main-Class" to "org.liamjd.bascule.MainKt",
				"Implementation-Title" to "Bascule static generator"
			)
		)
	}
}

tasks {
	build {
		dependsOn(shadowJar)
	}
}

/*
// configure shadowJar
shadowJar {
	classifier = null
	manifest {
		attributes(
			"Main-Class": "org.liamjd.bascule.MainKt",
		"Implementation-Title": "Bascule static generator"
		)
	}
}

// building source jar
task sourceJar(type: Jar) {
	from sourceSets.main.allJava
}
*/

