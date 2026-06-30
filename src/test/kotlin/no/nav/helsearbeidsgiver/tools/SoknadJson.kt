package no.nav.helsearbeidsgiver.tools

import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengeSoeknadKafkaMelding
import no.nav.helsearbeidsgiver.soeknad.Sykepengesoeknad
import no.nav.helsearbeidsgiver.utils.konverter

fun main() {
    konverteringLesFraStdin()
        .let { json.decodeFromString<SykepengeSoeknadKafkaMelding>(it) }
        .let { json.encodeToString<Sykepengesoeknad>(it.konverter(-1)) }
        .let { printOgKopierResultat(it) }
}