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

rootProject.name = "AtasMobile"

include(
    ":app",
    ":core:data",
    ":core:database",
    ":core:drive",
    ":core:ui",
    ":feature:meetings",
    ":feature:backup"
)
