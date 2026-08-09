plugins {
    plugins {
        alias(libs.plugins.kotlinJvm) apply false
        alias(libs.plugins.spring.boot) apply false
    }
}

// Ensure consistent JVM target for Java and Kotlin across subprojects
subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(17)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "17"
    }
}