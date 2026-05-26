import com.vanniktech.maven.publish.DeploymentValidation
import com.vanniktech.maven.publish.MavenPublishBaseExtension

val javaVersion = providers.gradleProperty("javaVersion").orElse("21").get().toInt()
val projectGroup = providers.gradleProperty("projectGroup").orElse("io.github.iamteppei").get()
val projectVersion = providers.gradleProperty("projectVersion").orElse("2026.05-04").get()
val mavenRepoUrl = providers.gradleProperty("mavenRepoUrl")
    .orElse("https://maven.pkg.github.com/iamteppei/simple-di")
    .get()
val sonatypeUsername = providers.gradleProperty("sonatypeUsername")
    .orElse(System.getenv("SONATYPE_USERNAME") ?: "")
    .get()
val sonatypePassword = providers.gradleProperty("sonatypePassword")
    .orElse(System.getenv("SONATYPE_PASSWORD") ?: "")
    .get()

// gradle requires plugin version to be defined globally
plugins {
    id("com.vanniktech.maven.publish") version "0.36.0" apply false
}

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    group = projectGroup
    version = projectVersion
}

subprojects {

    pluginManager.apply("java-library")
    pluginManager.apply("com.vanniktech.maven.publish")

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

    extensions.configure<MavenPublishBaseExtension> {
        coordinates(projectGroup, project.name, projectVersion)

        publishToMavenCentral(automaticRelease = true, validateDeployment = DeploymentValidation.NONE)
        signAllPublications() // request for release

        // configure pom
        pom {
            name.set(project.name)
            description.set("A simple dependency injection library")
            inceptionYear.set("2026")
            url.set("https://github.com/iamteppei/simple-di")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("iamteppei")
                    name.set("Tam Nguyen")
                    url.set("https://github.com/iamteppei/")
                }
            }
            scm {
                url.set("https://github.com/iamteppei/simple-di")
                connection.set("scm:git:git://github.com/iamteppei/simple-di.git")
                developerConnection.set("scm:git:ssh://git@github.com/iamteppei/simple-di.git")
            }
        }

    }
}