plugins {
    id("cloudphotos.android.library")
    id("cloudphotos.android.hilt")
}

android {
    namespace = "com.appvoyager.cloudphotos.feature.settings.data"
}

dependencies {
    implementation(project(":feature:settings:domain"))

    implementation(libs.androidx.datastore.preferences)
}
