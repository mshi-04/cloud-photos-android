import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")

            extensions.configure<LibraryExtension> {
                compileSdk = 36

                defaultConfig {
                    minSdk = 29
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
                    targetCompatibility = org.gradle.api.JavaVersion.VERSION_17
                    isCoreLibraryDesugaringEnabled = true
                }

                @Suppress("UnstableApiUsage")
                testOptions {
                    unitTests.all {
                        it.useJUnitPlatform()
                    }
                }
            }

            val libs = extensions.getByType(
                org.gradle.api.artifacts.VersionCatalogsExtension::class.java
            ).named("libs")

            dependencies {
                add("coreLibraryDesugaring", libs.findLibrary("desugar-jdk-libs").get())
            }
        }
    }
}
