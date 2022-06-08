import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

apply(from = "gradle.properties")
val bascule_lib_version: String by extra
val handlebars_version: String by extra
val slf4j_version: String by extra
val flexmark_version : String by extra
val spek_version: String by extra
val mockk_version: String by extra
val picocli_version: String by extra
val jansi_version: String by extra
val snakeyaml_version: String by extra
val koin_version: String by extra
val serialization_version: String by extra

buildscript {
}
plugins {
	kotlin("jvm") version "1.6.21"
	`maven-publish`
	id("com.github.johnrengelman.shadow") version "4.0.3"
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
	// bascule library
	implementation("org.liamjd.bascule-lib:bascule-lib:$bascule_lib_version")

	// handlebars templating
	implementation("com.github.jknack:handlebars:$handlebars_version")
//	testImplementation(kotlin("test"))

	// markdown - probably want to be more selective with this!
	implementation("com.vladsch.flexmark:flexmark-all:$flexmark_version")
	implementation("com.vladsch.flexmark:flexmark-ext-tables:$flexmark_version")

	// sl4j logging
	implementation("org.slf4j:slf4j-simple:$slf4j_version")
	implementation("io.github.microutils:kotlin-logging:1.6.26")

	// command line parsing
	implementation("info.picocli:picocli:$picocli_version")
	implementation("org.fusesource.jansi:jansi:$jansi_version")

	// Koin for Kotlin apps
	implementation("org.koin:koin-core:$koin_version")

	// for console progress bar?
	implementation("com.vdurmont:etaprinter:1.0.0")

	// kotlinx serialization, for document caching
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
//	implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serialization_version")
}

tasks.test {
	useJUnitPlatform() {
		includeEngines("spek2")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions.jvmTarget = "11"
}


