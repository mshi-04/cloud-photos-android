import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(AndroidLibraryConventionPlugin::class.java)
            pluginManager.apply(AndroidHiltConventionPlugin::class.java)
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<LibraryExtension> {
                buildFeatures {
                    compose = true
                }
            }

            val libs = extensions.getByType(
                org.gradle.api.artifacts.VersionCatalogsExtension::class.java
            ).named("libs")

            dependencies {
                add("implementation", platform(libs.findLibrary("androidx-compose-bom").get()))
                add("implementation", libs.findLibrary("androidx-ui").get())
                add("implementation", libs.findLibrary("androidx-ui-graphics").get())
                add("implementation", libs.findLibrary("androidx-material3").get())
                add("implementation", libs.findLibrary("androidx-ui-tooling-preview").get())
                add("debugImplementation", libs.findLibrary("androidx-ui-tooling").get())
                add("implementation", libs.findLibrary("androidx-hilt-navigation-compose").get())
            }
        }
    }
}
