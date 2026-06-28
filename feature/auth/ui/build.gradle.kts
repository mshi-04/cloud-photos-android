plugins {
    id("cloudphotos.android.feature")
}

android {
    namespace = "com.appvoyager.cloudphotos.feature.auth.ui"
}

dependencies {
    implementation(project(":feature:auth:domain"))
    implementation(project(":core:ui"))

    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
