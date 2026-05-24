import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

val javaVersion = providers.gradleProperty("javaVersion").orElse("21").get().toInt()

subprojects {
    pluginManager.apply("java-library")

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.compilerArgs = listOf(
            "-Xlint:serial",
            "-Xlint:varargs",
            "-Xlint:cast",
            "-Xlint:classfile",
            "-Xlint:dep-ann",
            "-Xlint:divzero",
            "-Xlint:empty",
            "-Xlint:finally",
            "-Xlint:overrides",
            "-Xlint:-path",
            "-Xlint:processing",
            "-Xlint:static",
            "-Xlint:try",
            "-Xlint:fallthrough",
            "-Xlint:rawtypes",
            "-Xlint:deprecation",
            "-Xlint:unchecked",
            "-Xlint:-options",
            "-Werror"
        )

        options.release.set(javaVersion)
        options.encoding = "UTF-8"
    }
}