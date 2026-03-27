import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

class KotlinLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(LintConventionPlugin::class.java)
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("dev.detekt")

            extensions.configure<KotlinJvmProjectExtension>("kotlin") {
                jvmToolchain(17)
            }

            extensions.configure<DetektExtension> {
                config.setFrom(rootProject.files("detekt.yml"))
                buildUponDefaultConfig.set(true)
                parallel.set(true)
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }
        }
    }
}
