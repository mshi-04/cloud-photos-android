plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.appvoyager.cloudphotos"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.appvoyager.cloudphotos"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FCM_TOPIC", "\"all_users\"")
    }

    flavorDimensions += "environment"
    productFlavors {
        BuildFlavor.entries.forEach { flavor ->
            create(flavor.flavorName) {
                dimension = "environment"
                flavor.appIdSuffix?.let { applicationIdSuffix = it }

                val cognitoClientId = requireEnvProperty(flavor, "COGNITO_CLIENT_ID")
                val apiBaseUrl = requireEnvProperty(flavor, "API_BASE_URL")
                val s3BucketName = requireEnvProperty(flavor, "S3_BUCKET_NAME")

                buildConfigField("String", "COGNITO_CLIENT_ID", "\"$cognitoClientId\"")
                buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
                buildConfigField("String", "S3_BUCKET_NAME", "\"$s3BucketName\"")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.ui.tooling)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    // AWS Amplify
    implementation(libs.amplify.core)
    implementation(libs.amplify.auth.cognito)
    implementation(libs.amplify.storage.s3)

    // Image Loading
    implementation(libs.coil.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Test
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

}

enum class BuildFlavor(
    val flavorName: String,
    val propertyPrefix: String,
    val appIdSuffix: String? = null
) {
    DEV("dev", "DEV", ".dev"),
    PROD("prod", "PROD", null)
}

private fun requireEnvProperty(flavor: BuildFlavor, baseName: String): String {
    val propertyName = "${flavor.propertyPrefix}_$baseName"
    val value = findProperty(propertyName)?.toString()?.takeIf { it.isNotBlank() }

    if (value != null) return value

    val isCodeQlAnalysis = System.getenv("CODEQL_ACTION_VERSION") != null
            || System.getenv("CODEQL_DIST") != null
    if (isCodeQlAnalysis) return ""

    if (!isFlavorValidationRequired(flavor)) return ""
    throw GradleException("$propertyName must be set for ${flavor.flavorName} builds")
}

private fun isFlavorValidationRequired(flavor: BuildFlavor): Boolean {
    val taskNames = gradle.startParameter.taskNames
        .map { it.substringAfterLast(":").lowercase() }
    if (taskNames.isEmpty()) return false

    val allFlavorTasks = setOf("assemble", "build", "bundle")
    return taskNames.any { task ->
        task in allFlavorTasks || task.contains(flavor.flavorName.lowercase())
    }
}
