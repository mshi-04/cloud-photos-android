plugins {
    id("cloudphotos.android.library")
    id("cloudphotos.android.hilt")
}

android {
    namespace = "com.appvoyager.cloudphotos.core.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.amplify.core)
    implementation(libs.amplify.api)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.turbine)
}
