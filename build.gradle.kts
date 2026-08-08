plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

// Ensure consistent JVM target for Java and Kotlin across subprojects
subprojects {
    // Set Java compile target (release) to 21
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
    }

    // Set Kotlin jvm target to 21
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "21"
    }
}

