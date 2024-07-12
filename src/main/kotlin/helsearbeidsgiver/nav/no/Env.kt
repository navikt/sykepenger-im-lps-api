package helsearbeidsgiver.nav.no

interface IEnv {
    val wellKnownUrl: String
    val scopes: String
}
class Env: IEnv {
    override val wellKnownUrl = "MASKINPORTEN_WELL_KNOWN_URL".fromEnv()
    override val scopes = "MASKINPORTEN_SCOPES".fromEnv()
}

class MockEnv: IEnv {
    override val wellKnownUrl = "http://localhost:33445/maskinporten/.well-known/openid-configuration"
    override val scopes = "nav:inntektsmelding/lps.write"
}

fun String.fromEnv(): String =
    System.getenv(this)
        ?: throw RuntimeException("Missing required environment variable \"$this\".")
