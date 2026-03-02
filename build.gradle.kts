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
    // Inject only the required build properties into each subproject.
    // Using an explicit whitelist instead of dumping all local.properties keys
    // prevents unintended values (e.g. sdk.dir) from leaking across modules.
    listOf(
        "DEV_API_BASE_URL",
        "DEV_COGNITO_CLIENT_ID",
        "DEV_S3_BUCKET_NAME",
        "PROD_API_BASE_URL",
        "PROD_COGNITO_CLIENT_ID",
        "PROD_S3_BUCKET_NAME"
    ).forEach { key ->
        val value = localProperties.getProperty(key)
        if (value != null && !extra.has(key)) {
            extra[key] = value
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