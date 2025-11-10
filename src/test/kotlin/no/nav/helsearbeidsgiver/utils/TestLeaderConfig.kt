package no.nav.helsearbeidsgiver.utils

fun getTestLeaderConfig(isLeader: Boolean): LeaderConfig =
    if (isLeader) {
        Leader
    } else {
        NotLeader
    }

private object Leader : LeaderConfig {
    override fun isElectedLeader(): Boolean = true
}

private object NotLeader : LeaderConfig {
    override fun isElectedLeader(): Boolean = false
}
