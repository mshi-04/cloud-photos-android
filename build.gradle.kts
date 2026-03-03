import java.util.Properties

val localProperties = Properties()
val localPropertiesFile: File = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.reader().use { localProperties.load(it) }
}

allprojects {
    listOf(
        "DEV_API_BASE_URL",
        "DEV_COGNITO_CLIENT_ID",
        "DEV_S3_BUCKET_NAME",
        "PROD_API_BASE_URL",
        "PROD_COGNITO_CLIENT_ID",
        "PROD_S3_BUCKET_NAME"
    ).forEach { key ->
        val value = localProperties.getProperty(key)
        if (value != null && findProperty(key) == null) {
            extra[key] = value
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
