plugins {
    id("cloudphotos.android.library")
    id("cloudphotos.android.hilt")
}

android {
    namespace = "com.appvoyager.cloudphotos.feature.auth.data"
}

dependencies {
    implementation(project(":feature:auth:domain"))

    implementation(libs.amplify.core)
    implementation(libs.amplify.auth.cognito)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
