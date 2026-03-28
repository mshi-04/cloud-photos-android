plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kotlinLibrary") {
            id = "cloudphotos.kotlin.library"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
        register("androidLibrary") {
            id = "cloudphotos.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidHilt") {
            id = "cloudphotos.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "cloudphotos.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("lint") {
            id = "cloudphotos.lint"
            implementationClass = "LintConventionPlugin"
        }
    }
}
