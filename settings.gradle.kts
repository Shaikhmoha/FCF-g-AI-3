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
    }
}

rootProject.name = "FCFVideoConverter"

include(":app")
include(":core")
include(":domain")
include(":data")
include(":media_engine")
include(":ai_engine")
include(":feature_converter")
include(":feature_compressor")
include(":feature_voice")
include(":feature_translation")
include(":feature_dubbing")
include(":feature_subtitles")
include(":feature_editor")
include(":feature_projects")
include(":feature_settings")
