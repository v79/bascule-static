import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.liamjd"
version = "0.3.3"

val kotlin_version = "1.6.21"
val snakeyaml_version = "1.23"
val mockk_version = "1.12.4"
val flexmark_version = "0.61.0"
val slf4j_version = "1.7.26"
val handlebars_version = "4.3.1"
val spek_version = "2.0.7"
val picocli_version = "3.8.2"
val jansi_version = "1.17.1"
val bascule_lib_version = "0.3.2"

plugins {
    kotlin("jvm") version "1.6.21"
    `maven-publish`
    id("com.github.johnrengelman.shadow") version "6.1.0"
    kotlin("plugin.serialization") version "1.6.21"
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // stdlib
    implementation(kotlin("stdlib","1.6.21"))
    // reflection
    api(kotlin("reflect","1.6.21"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.1")

    // bascule library
    implementation("org.liamjd.bascule-lib:bascule-lib:$bascule_lib_version")

    // handlebars templating
    implementation("com.github.jknack:handlebars:$handlebars_version")

    // markdown - probably want to be more selective with this!
    implementation("com.vladsch.flexmark:flexmark-all:$flexmark_version")
    implementation("com.vladsch.flexmark:flexmark-ext-tables:$flexmark_version")
    implementation("com.vladsch.flexmark:flexmark-util-builder:$flexmark_version")

    // snakeyaml
    implementation("org.yaml:snakeyaml:$snakeyaml_version")

    // sl4j logging
    implementation("org.slf4j:slf4j-simple:$slf4j_version")
    implementation("io.github.microutils:kotlin-logging:1.6.26")

    // command line parsing
    implementation("info.picocli:picocli:$picocli_version")
    implementation("org.fusesource.jansi:jansi:$jansi_version")

    // Koin for Kotlin apps
    implementation("io.insert-koin:koin-core:3.0.2")

    // kotlinx serialization, for document caching
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    // for console progress bar?
    implementation("com.vdurmont:etaprinter:1.0.0")

    // Testing
    implementation("io.insert-koin:koin-test:3.0.2")

    // testing
//    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")

    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spek_version")
    testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spek_version")

    testImplementation("io.mockk:mockk:${mockk_version}")

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
    minimize()
    manifest {
        attributes(
            "Main-Class" to "org.liamjd.bascule.MainKt",
            "Implementation-Title" to "Bascule static generator"
        )
    }
}


// publishing
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
