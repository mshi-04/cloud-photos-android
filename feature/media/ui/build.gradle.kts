plugins {
    id("cloudphotos.android.feature")
}

android {
    namespace = "com.appvoyager.cloudphotos.feature.media.ui"
}

dependencies {
    implementation(project(":feature:media:domain"))
    implementation(project(":feature:settings:domain"))
    implementation(project(":core:ui"))

    implementation(libs.coil.compose)
    implementation(libs.androidx.material.icons.extended)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
