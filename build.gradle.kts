import java.util.Properties

// Load local.properties and inject entries into all projects so that
// project.findProperty() in subprojects resolves values transparently,
// just like gradle.properties or -P CLI arguments.
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.reader().use { localProperties.load(it) }
}

allprojects {
    localProperties.forEach { key, value ->
        val keyStr = key.toString()
        if (!extra.has(keyStr)) {
            extra[keyStr] = value
        }
    }
}

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}