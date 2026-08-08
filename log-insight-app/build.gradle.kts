plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

// Enforce Java 17 toolchain for compilation and execution compatibility
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Declaring project dependency to our core domain module
    implementation(project(":log-insight-core"))
    implementation(libs.kotlin.coroutines.core)
}

application {
    // Configures the main class for gradle run task
    mainClass.set("pl.blaszak.loginsight.app.AppKt")
}
