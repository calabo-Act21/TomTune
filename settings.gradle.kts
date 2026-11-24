pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "TomTune"
include(":shared")
if (providers.gradleProperty("mobile").orNull != "false") include(":composeApp")
