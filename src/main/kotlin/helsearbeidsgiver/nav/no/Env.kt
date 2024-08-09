package helsearbeidsgiver.nav.no

object Env {
    val wellKnownUrl = "MASKINPORTEN_WELL_KNOWN_URL".fromEnv()
    val scopes = "MASKINPORTEN_SCOPES".fromEnv()
}


fun String.fromEnv(): String =
    System.getenv(this)
        ?: throw RuntimeException("Missing required environment variable \"$this\".")
