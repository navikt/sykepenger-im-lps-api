package no.nav.helsearbeidsgiver.tools

import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.model.Sykmelding
import no.nav.helsearbeidsgiver.sykmelding.model.tilSykmelding
import no.nav.helsearbeidsgiver.sykmelding.tilSykmeldingDTO

fun main() {
    konverteringLesFraStdin()
        .let { json.decodeFromString<SendSykmeldingAivenKafkaMessage>(it) }
        .let { it.tilSykmeldingDTO(loepenr = -1) }
        .let { json.encodeToString<Sykmelding>(it.tilSykmelding()) }
        .let { printOgKopierResultat(it) }
}
