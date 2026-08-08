plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Enforce Java 17 toolchain for compilation and testing compatibility
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(libs.kotlin.coroutines.core) // Dodana zależność do coroutines

    // Test dependencies
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
}

tasks.test {
    useJUnitPlatform()
}