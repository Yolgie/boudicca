include("eventcollector-api")
include("eventcollectors")
include("publisher-api")
include("publisher-ical")
include("publisher-html")
include("core")
include("core-openapi")
include("search")
include("search-openapi")
include("semantic-conventions")

pluginManagement {
    val quarkusPluginVersion: String by settings
    val quarkusPluginId: String by settings
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
    plugins {
        id(quarkusPluginId) version quarkusPluginVersion
    }
}
rootProject.name = "boudicca"