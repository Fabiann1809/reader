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
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Only for the PDFium native library that Readium's PDF adapter needs (not on Maven Central).
        // The filter keeps any other dependency from ever being resolved from JitPack.
        maven("https://jitpack.io") {
            content { includeGroup("com.github.marain87") }
        }
    }
}

rootProject.name = "reader"
include(":app")
