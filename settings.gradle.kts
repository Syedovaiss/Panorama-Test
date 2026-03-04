pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
        google()
        maven("https://repo.grails.org/grails/core/" )
        maven("https://jitpack.io")
        maven("https://maven.google.com")
        mavenCentral()
        maven("https://maven.fabric.io/public")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        google()
        maven("https://repo.grails.org/grails/core/" )
        maven("https://jitpack.io")
        maven("https://maven.google.com")
        mavenCentral()
        maven("https://maven.fabric.io/public")
    }
}

rootProject.name = "Panorama"
include(":app")
include(":open-cv")
include(":camera_360")
include(":panorama_strip")
include(":nativecore")
include(":panoramacv")
