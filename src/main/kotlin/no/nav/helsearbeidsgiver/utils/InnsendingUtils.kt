package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Avsender
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Sykmeldt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.api.AvsenderSystem
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.api.Innsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRequest
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.OffsetDateTime
import java.util.UUID

fun Inntektsmelding.erDuplikat(other: Inntektsmelding) =
    this == other.copy(
        avsender = avsender,
        mottatt = mottatt,
        aarsakInnsending = aarsakInnsending,
    )

fun InntektsmeldingRequest.tilInntektsmelding(sluttbrukerOrgnr: Orgnr, lpsOrgnr: Orgnr, forespoersel: Forespoersel): Inntektsmelding =
    Inntektsmelding(
        id = UUID.randomUUID(),
        type = Inntektsmelding.Type.ForespurtEkstern(
            navReferanseId,
            AvsenderSystem(
                lpsOrgnr,
                avsender.systemNavn,
                avsender.systemVersjon,
            )
        ),
        sykmeldt = Sykmeldt(Fnr(sykmeldtFnr), ""),
        avsender = Avsender(
            sluttbrukerOrgnr,
            "",
            "",
            arbeidsgiverTlf
        ),
        sykmeldingsperioder = forespoersel.sykmeldingsperioder,
        agp = agp,
        inntekt = inntekt,
        refusjon = refusjon,
        aarsakInnsending = aarsakInnsending,
        mottatt = OffsetDateTime.now(),
        vedtaksperiodeId = null,
    )

fun InntektsmeldingRequest.tilInnsending(type: Inntektsmelding.Type, versjon: Int): Innsending {
    val skjemaInntektsmelding =
        SkjemaInntektsmelding(
            forespoerselId = navReferanseId,
            avsenderTlf = arbeidsgiverTlf,
            agp = agp,
            inntekt = inntekt,
            refusjon = refusjon,
        )
    return Innsending(
        innsendingId = UUID.randomUUID(),
        skjema = skjemaInntektsmelding,
        aarsakInnsending = aarsakInnsending,
        type = type,
        innsendtTid = OffsetDateTime.now(),
        versjon = versjon
    )
}
