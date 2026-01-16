pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AgendaMobile"

include(
    ":app",
    ":core:data",
    ":core:database",
    ":core:drive",
    ":core:sync",
    ":core:ui",
    ":feature:meetings",
    ":feature:backup"
)
