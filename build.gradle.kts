import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.liamjd"
version = "0.4.0"

val kotlin_version = "1.9.22"
val snakeyaml_version = "2.4"
val mockk_version = "1.12.4"
val flexmark_version = "0.64.8"
val slf4j_version = "1.7.26"
val handlebars_version = "4.4.0"
val picocli_version = "3.8.2"
val jansi_version = "1.17.1"
val bascule_lib_version = "0.4.0"
val junit_version = "5.10.2"
val koin_version = "3.5.6"

plugins {
    kotlin("jvm") version "1.9.22"
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.8"
    kotlin("plugin.serialization") version "1.9.22"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://maven.pkg.github.com/v79/bascule-lib")
        credentials {
            username = (project.findProperty("githubUsername") ?: System.getenv("GITHUB_ACTOR")) as String?
            password = (project.findProperty("githubToken") ?: System.getenv("GH_PAT_REPO")) as String?
        }
    }
}

dependencies {
    // stdlib
    implementation(kotlin("stdlib", kotlin_version))
    // reflection
    api(kotlin("reflect", kotlin_version))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.1")

    // bascule library
    implementation("org.liamjd.bascule:bascule-lib:$bascule_lib_version")

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
    implementation(project.dependencies.platform("io.insert-koin:koin-bom:$koin_version"))
    implementation("io.insert-koin:koin-core")

    // kotlinx serialization, for document caching
    runtimeOnly("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    // for console progress bar?
    implementation("com.vdurmont:etaprinter:1.0.0")

    // Testing
// Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:${kotlin_version}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junit_version}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junit_version}")
    testImplementation("io.mockk:mockk:${mockk_version}")
    testImplementation("io.insert-koin:koin-test:${koin_version}")
    testImplementation("io.insert-koin:koin-test-junit5:${koin_version}")

}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
    }
}

tasks.shadowJar {
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
            groupId = "org.liamjd"
            artifactId = "bascule"
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/v79/bascule-static")
            credentials {
                username = (project.findProperty("gpr.user") ?: System.getenv("GH_USERNAME")) as String?
                password = (project.findProperty("gpr.key") ?: System.getenv("GH_PAT_REPO")) as String?
            }
        }
    }
}
