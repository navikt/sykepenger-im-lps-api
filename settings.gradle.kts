rootProject.name = "sykepenger-im-lps-api"

pluginManagement {
    plugins {
        val kotlinterVersion = providers.gradleProperty("kotlinterVersion").get()
        val kotlinVersion = providers.gradleProperty("kotlinVersion").get()
        val ktorVersion = providers.gradleProperty("ktorVersion").get()

        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jmailen.kotlinter") version kotlinterVersion
        id("io.ktor.plugin") version ktorVersion
    }
}
