package no.nav.helsearbeidsgiver.tools

import no.nav.helsearbeidsgiver.kafka.soeknad.SykepengeSoeknadKafkaMelding
import no.nav.helsearbeidsgiver.soeknad.SykepengesoeknadForPDF
import no.nav.helsearbeidsgiver.utils.konverter

fun main() {
    konverteringLesFraStdin()
        .let { json.decodeFromString<SykepengeSoeknadKafkaMelding>(it) }
        .let { json.encodeToString<SykepengesoeknadForPDF>(SykepengesoeknadForPDF(it.konverter(-1), "default navn")) }
        .let { printOgKopierResultat(it) }
}
