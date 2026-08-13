import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
        alias(libs.plugins.kotlinJvm) apply false
        alias(libs.plugins.spring.boot) apply false
}

// Ensure consistent JVM target for Java and Kotlin across subprojects
subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.release.set(21)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}