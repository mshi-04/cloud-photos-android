import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

class LintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")
            pluginManager.apply("dev.detekt")

            extensions.configure<KtlintExtension> {
                android.set(true)
                outputToConsole.set(true)
                ignoreFailures.set(false)
            }

            extensions.configure<DetektExtension> {
                config.setFrom(rootProject.files("detekt.yml"))
                buildUponDefaultConfig.set(true)
                parallel.set(true)
            }
        }
    }
}
