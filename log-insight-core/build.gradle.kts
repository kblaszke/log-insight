import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(libs.versions.javaVersion.get()))
    }
}

dependencies {
    implementation(libs.kotlin.coroutines.core)

    // Test dependencies
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}