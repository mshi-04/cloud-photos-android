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
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FCM_TOPIC", "\"all_users\"")
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"

            val devCognitoClientId = requireNonBlankProperty("DEV_COGNITO_CLIENT_ID", "development")
            val devApiBaseUrl = requireNonBlankProperty("DEV_API_BASE_URL", "development")
            val devS3BucketName = requireNonBlankProperty("DEV_S3_BUCKET_NAME", "development")

            buildConfigField("String", "COGNITO_CLIENT_ID", "\"$devCognitoClientId\"")
            buildConfigField("String", "API_BASE_URL", "\"$devApiBaseUrl\"")
            buildConfigField("String", "S3_BUCKET_NAME", "\"$devS3BucketName\"")
        }
        create("prod") {
            dimension = "environment"

            val prodCognitoClientId = requireNonBlankProperty("PROD_COGNITO_CLIENT_ID", "production")
            val prodApiBaseUrl = requireNonBlankProperty("PROD_API_BASE_URL", "production")
            val prodS3BucketName = requireNonBlankProperty("PROD_S3_BUCKET_NAME", "production")

            buildConfigField("String", "COGNITO_CLIENT_ID", "\"$prodCognitoClientId\"")
            buildConfigField("String", "API_BASE_URL", "\"$prodApiBaseUrl\"")
            buildConfigField("String", "S3_BUCKET_NAME", "\"$prodS3BucketName\"")
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

private fun requireNonBlankProperty(name: String, target: String): String {
    val value = findProperty(name)?.toString()?.takeIf { it.isNotBlank() }
    if (value != null) return value

    val isTargetBuild = gradle.startParameter.taskNames.any {
        it.contains(target, ignoreCase = true)
    }
    if (isTargetBuild) {
        throw GradleException("$name must be set for $target builds")
    }

    return "UNCONFIGURED"
}
