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
        maven {
            url = uri("https://www.jitpack.io" )
        }
        maven{
            url = java.net.URI("https://devrepo.kakao.com/nexus/content/groups/public/")
        }
        google()
        mavenCentral()


    }
}
rootProject.name = "project1"
include(":app")
