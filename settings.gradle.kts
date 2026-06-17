rootProject.name = "sykepenger-im-lps-api"

pluginManagement {
    plugins {
        val kotlinterVersion: String by settings
        val kotlinVersion: String by settings
        val ktorVersion: String by settings

        kotlin("jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("org.jmailen.kotlinter") version kotlinterVersion
        id("io.ktor.plugin") version ktorVersion
    }
}
