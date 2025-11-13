// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.gradle.api.JavaVersion

plugins {
    alias(libs.plugins.androidApplication) apply false
}

// Fuerza toolchain Java 17 para Gradle y todos los subproyectos
allprojects {
    plugins.withId("java") {
        (this as org.gradle.api.plugins.JavaPluginExtension).toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}
