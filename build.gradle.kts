import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "org.liamjd"
version = "0.5.1"

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    alias(libs.plugins.shadow)
    alias(libs.plugins.kotlin.serialization)
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
    // reflection
    api(libs.kotlin.reflect)

    implementation(libs.kotlinx.coroutines.core)

    // bascule library
    implementation(libs.bascule.lib)

    // handlebars templating
    implementation(libs.handlebars)

    // markdown
    implementation(libs.flexmark.all)
    implementation(libs.flexmark.ext.tables)
    implementation(libs.flexmark.util.builder)

    // snakeyaml
    implementation(libs.snakeyaml)

    // sl4j logging
    implementation(libs.slf4j.simple)
    implementation(libs.kotlin.logging)

    // command line parsing
    implementation(libs.picocli)
    implementation(libs.jansi)

    // Koin for Kotlin apps
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)

    // kotlinx serialization, for document caching
    runtimeOnly(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.serialization.json)

    // Testing
    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit5)
}

tasks.test {
    useJUnitPlatform()
    // Allow ByteBuddy (used by MockK to mock final classes) to operate on newer JDKs than it was built against
    systemProperty("net.bytebuddy.experimental", "true")
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
