plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.spring.boot)
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = libs.versions.javaVersion.get()
    }
}

dependencies {
    // Project dependency to core
    implementation(project(":log-insight-core"))

    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.coroutines.core)

    // Reactive Spring WebFlux
    implementation(libs.spring.boot.starter.webflux)

    // Jackson Kotlin module for proper value class serialization
    implementation(libs.jackson.module.kotlin)

    // SpringDoc OpenAPI / Swagger UI
    implementation(libs.springdoc.openapi.webflux.ui)

    // Test dependencies
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.spring.extension)
}

application {
    mainClass.set("pl.blaszak.loginsight.app.AppKt")
}

tasks.test {
    useJUnitPlatform()
}