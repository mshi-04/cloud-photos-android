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

            val devCognitoClientId = project.findProperty("DEV_COGNITO_CLIENT_ID")?.toString()
                ?: throw GradleException("DEV_COGNITO_CLIENT_ID must be set for development builds")
            val devApiBaseUrl = project.findProperty("DEV_API_BASE_URL")?.toString()
                ?: throw GradleException("DEV_API_BASE_URL must be set for development builds")
            val devS3BucketName = project.findProperty("DEV_S3_BUCKET_NAME")?.toString()
                ?: throw GradleException("DEV_S3_BUCKET_NAME must be set for development builds")

            buildConfigField("String", "COGNITO_CLIENT_ID", "\"$devCognitoClientId\"")
            buildConfigField("String", "API_BASE_URL", "\"$devApiBaseUrl\"")
            buildConfigField("String", "S3_BUCKET_NAME", "\"$devS3BucketName\"")
        }
        create("prod") {
            dimension = "environment"

            val prodCognitoClientId = project.findProperty("PROD_COGNITO_CLIENT_ID")?.toString()
                ?: throw GradleException("PROD_COGNITO_CLIENT_ID must be set for production builds")
            val prodApiBaseUrl = project.findProperty("PROD_API_BASE_URL")?.toString()
                ?: throw GradleException("PROD_API_BASE_URL must be set for production builds")
            val prodS3BucketName = project.findProperty("PROD_S3_BUCKET_NAME")?.toString()
                ?: throw GradleException("PROD_S3_BUCKET_NAME must be set for production builds")

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
