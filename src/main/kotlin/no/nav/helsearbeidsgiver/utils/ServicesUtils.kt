package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.api.Innsending
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import org.jetbrains.exposed.sql.transactions.transaction

fun Services.opprettImTransaction(
    inntektsmelding: Inntektsmelding,
    innsending: Innsending,
) {
    transaction {
        inntektsmeldingService.opprettInntektsmelding(
            im = inntektsmelding,
            innsendingStatus = InnsendingStatus.MOTTATT,
        )
        innsendingService.lagreBakgrunsjobbInnsending(
            innsending = innsending,
        )
    }
}
