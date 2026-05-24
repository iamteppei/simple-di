import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion

val javaVersion = providers.gradleProperty("javaVersion").orElse("21").get().toInt()
val projectGroup = providers.gradleProperty("projectGroup").orElse("io.github.iamteppei").get()
val projectVersion = providers.gradleProperty("projectVersion").orElse("0.1.0-SNAPSHOT").get()
val mavenRepoUrl = providers.gradleProperty("mavenRepoUrl")
    .orElse("https://maven.pkg.github.com/iamteppei/simple-di")
    .get()

allprojects {
    group = projectGroup
    version = projectVersion
}

subprojects {
    pluginManager.apply("java-library")
    pluginManager.apply("maven-publish")

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

    extensions.configure<PublishingExtension> {
        publications {
            create<MavenPublication>("mavenJava") {
                from(components["java"])
                artifactId = project.name
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri(mavenRepoUrl)
                credentials {
                    username = providers.gradleProperty("mavenRepoUsername")
                        .orElse(System.getenv("GITHUB_ACTOR") ?: "")
                        .get()
                    password = providers.gradleProperty("mavenRepoPassword")
                        .orElse(System.getenv("GITHUB_TOKEN") ?: "")
                        .get()
                }
            }
        }
    }
}