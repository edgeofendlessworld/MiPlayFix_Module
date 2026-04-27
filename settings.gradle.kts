pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        mavenLocal {
            content {
                includeGroup("com.xposed")
            }
        }
    }
    versionCatalogs {
        create("libs")
    }
}

rootProject.name = "miplayfix"

include(":app")
