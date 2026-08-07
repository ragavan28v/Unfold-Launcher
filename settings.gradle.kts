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
// Toolchain resolver plugin removed to avoid platform-specific jlink/toolchain
// resolution issues on developer machines. Builds will use the system JDK
// or the JDK configured by the developer's Gradle/IDE settings.
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
