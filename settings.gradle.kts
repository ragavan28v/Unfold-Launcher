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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Unfold"
include(":app")
include(":core:core-ui")
include(":core:core-domain")
include(":core:core-data")
include(":feature:feature-home")
include(":feature:feature-drawer")
include(":feature:feature-gestures")
include(":feature:feature-hidden-space")
include(":feature:feature-widgets")
include(":feature:feature-notifications")
include(":feature:feature-settings")
include(":feature:feature-search")
