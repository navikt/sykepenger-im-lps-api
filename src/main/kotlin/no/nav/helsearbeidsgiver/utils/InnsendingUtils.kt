package no.nav.helsearbeidsgiver.utils

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Avsender
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Sykmeldt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.api.AvsenderSystem
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.api.Innsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRequest
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.OffsetDateTime
import java.util.UUID

fun SkjemaInntektsmelding.erDuplikat(other: SkjemaInntektsmelding) =
    this ==
        other.copy(
            avsenderTlf = avsenderTlf,
        )

fun InntektsmeldingRequest.tilInntektsmelding(
    sluttbrukerOrgnr: Orgnr,
    lpsOrgnr: Orgnr,
    forespoersel: Forespoersel,
    vedtaksperiodeId: UUID?,
): Inntektsmelding =
    Inntektsmelding(
        id = UUID.randomUUID(),
        type =
            Inntektsmelding.Type.ForespurtEkstern(
                navReferanseId,
                AvsenderSystem(
                    lpsOrgnr,
                    avsender.systemNavn,
                    avsender.systemVersjon,
                ),
            ),
        sykmeldt = Sykmeldt(Fnr(sykmeldtFnr), ""),
        avsender =
            Avsender(
                sluttbrukerOrgnr,
                "",
                "",
                arbeidsgiverTlf,
            ),
        sykmeldingsperioder = forespoersel.sykmeldingsperioder,
        agp = agp,
        inntekt = inntekt,
        refusjon = refusjon,
        aarsakInnsending = aarsakInnsending,
        mottatt = OffsetDateTime.now(),
        vedtaksperiodeId = vedtaksperiodeId,
    )

fun InntektsmeldingRequest.tilInnsending(
    inntektsmeldingId: UUID,
    type: Inntektsmelding.Type,
    versjon: Int,
): Innsending {
    val skjemaInntektsmelding =
        SkjemaInntektsmelding(
            forespoerselId = navReferanseId,
            avsenderTlf = arbeidsgiverTlf,
            agp = agp,
            inntekt = inntekt,
            refusjon = refusjon,
        )
    return Innsending(
        innsendingId = inntektsmeldingId,
        skjema = skjemaInntektsmelding,
        aarsakInnsending = aarsakInnsending,
        type = type,
        innsendtTid = OffsetDateTime.now(),
        versjon = versjon,
    )
}

fun InntektsmeldingResponse.tilSkjemaInntektsmelding() =
    SkjemaInntektsmelding(
        forespoerselId = navReferanseId,
        avsenderTlf = arbeidsgiver.tlf,
        agp = agp,
        inntekt = inntekt,
        refusjon = refusjon,
    )

fun InntektsmeldingRequest.validerMotForespoersel(
    forespoersel: Forespoersel,
    sluttbrukerOrgnr: String,
): String? =
    when {
        forespoersel.orgnr != sluttbrukerOrgnr -> Feilmelding.FEIL_ORGNR
        forespoersel.fnr != this.sykmeldtFnr -> Feilmelding.FEIL_FNR
        forespoersel.inntektPaakrevd && this.inntekt == null -> Feilmelding.INNTEKT_ER_PAAKREVD
        !forespoersel.inntektPaakrevd && this.inntekt != null -> Feilmelding.INNTEKT_ER_IKKE_PAAKREVD
        forespoersel.arbeidsgiverperiodePaakrevd && this.agp == null -> Feilmelding.AGP_ER_PAAKREVD
        !forespoersel.arbeidsgiverperiodePaakrevd && this.agp == null -> Feilmelding.AGP_ER_IKKE_PAAKREVD
        forespoersel.status == Status.AKTIV && this.aarsakInnsending == AarsakInnsending.Endring -> Feilmelding.UGYLDIG_AARSAK
        forespoersel.status == Status.BESVART && this.aarsakInnsending == AarsakInnsending.Ny -> Feilmelding.UGYLDIG_AARSAK
        forespoersel.status == Status.FORKASTET -> Feilmelding.FORESPOERSEL_FORKASTET
        else -> null
    }

internal object Feilmelding {
    const val FEIL_ORGNR = "Feil organisasjonsnummer"
    const val FEIL_FNR = "Feil foedselsnummer"
    const val INNTEKT_ER_PAAKREVD = "Inntekt er paakrevd"
    const val AGP_ER_PAAKREVD = "Agp er paakrevd"
    const val INNTEKT_ER_IKKE_PAAKREVD = "Inntekt er ikke paakrevd"
    const val AGP_ER_IKKE_PAAKREVD = "Agp er ikke paakrevd"
    const val UGYLDIG_AARSAK = "Ugyldig aarsak innsending"
    const val FORESPOERSEL_FORKASTET = "Forespoersel er trukket tilbake"
}
