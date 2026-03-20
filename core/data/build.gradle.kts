plugins {
    id("cloudphotos.android.library")
}

android {
    namespace = "com.appvoyager.cloudphotos.core.data"
}

dependencies {
    implementation(project(":core:common"))
    implementation(libs.javax.inject)
}
