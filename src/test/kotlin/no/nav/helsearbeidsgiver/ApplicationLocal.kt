package no.nav.helsearbeidsgiver

import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

fun main() {
    sikkerLogger().info("*** Starter server lokalt***")
    startServer()
}
