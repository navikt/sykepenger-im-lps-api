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
                forespoersel.arbeidsgiverperiodePaakrevd,
                AvsenderSystem(
                    lpsOrgnr,
                    avsender.systemNavn,
                    avsender.systemVersjon,
                ),
            ),
        sykmeldt = Sykmeldt(Fnr(sykmeldtFnr), ""),
        avsender =
            Avsender(
                orgnr = sluttbrukerOrgnr,
                orgNavn = "",
                navn = kontaktinformasjon,
                tlf = arbeidsgiverTlf,
            ),
        sykmeldingsperioder = forespoersel.sykmeldingsperioder,
        agp = agp,
        inntekt = inntekt,
        naturalytelser = naturalytelser,
        refusjon = refusjon,
        aarsakInnsending = aarsakInnsending,
        mottatt = OffsetDateTime.now(),
        vedtaksperiodeId = vedtaksperiodeId,
    )

fun InntektsmeldingRequest.tilInnsending(
    inntektsmeldingId: UUID,
    eksponertForespoerselId: UUID,
    type: Inntektsmelding.Type,
    versjon: Int,
): Innsending {
    val skjemaInntektsmelding =
        SkjemaInntektsmelding(
            forespoerselId = eksponertForespoerselId,
            avsenderTlf = arbeidsgiverTlf,
            agp = agp,
            inntekt = inntekt,
            naturalytelser = naturalytelser,
            refusjon = refusjon,
        )
    return Innsending(
        innsendingId = inntektsmeldingId,
        skjema = skjemaInntektsmelding,
        aarsakInnsending = aarsakInnsending,
        type = type,
        innsendtTid = OffsetDateTime.now(),
        kontaktinfo = kontaktinformasjon,
        versjon = versjon,
    )
}

fun InntektsmeldingResponse.tilSkjemaInntektsmelding(eksponertForespoerselId: UUID) =
    SkjemaInntektsmelding(
        forespoerselId = eksponertForespoerselId,
        avsenderTlf = arbeidsgiver.tlf,
        agp = agp,
        inntekt = inntekt,
        naturalytelser = naturalytelser,
        refusjon = refusjon,
    )

fun InntektsmeldingRequest.validerMotForespoersel(forespoersel: Forespoersel): String? =
    when {
        forespoersel.navReferanseId != navReferanseId -> Feilmelding.UGYLDIG_REFERANSE // sjekker for sikkerhets skyld
        forespoersel.fnr != this.sykmeldtFnr -> Feilmelding.FEIL_FNR
        forespoersel.inntektPaakrevd && this.inntekt == null -> Feilmelding.INNTEKT_ER_PAAKREVD
        !forespoersel.inntektPaakrevd && this.inntekt != null -> Feilmelding.INNTEKT_ER_IKKE_PAAKREVD
        forespoersel.arbeidsgiverperiodePaakrevd && this.agp == null -> Feilmelding.AGP_ER_PAAKREVD
        !forespoersel.arbeidsgiverperiodePaakrevd && agp != null &&
            !agp.erGyldigHvisIkkeForespurt(
                false,
                forespoersel.sykmeldingsperioder,
            )
        -> Feilmelding.AGP_IKKE_FORESPURT_ER_UGYLDIG
        forespoersel.status == Status.AKTIV && this.aarsakInnsending == AarsakInnsending.Endring -> Feilmelding.UGYLDIG_AARSAK
        forespoersel.status == Status.BESVART && this.aarsakInnsending == AarsakInnsending.Ny -> Feilmelding.UGYLDIG_AARSAK
        forespoersel.status == Status.FORKASTET -> Feilmelding.FORESPOERSEL_FORKASTET
        else -> null
    }

object Feilmelding {
    const val FEIL_ORGNR = "Feil organisasjonsnummer"
    const val FEIL_FNR = "Feil foedselsnummer"
    const val INNTEKT_ER_PAAKREVD = "Inntekt er paakrevd"
    const val AGP_ER_PAAKREVD = "AGP er paakrevd"
    const val INNTEKT_ER_IKKE_PAAKREVD = "Inntekt er ikke paakrevd"
    const val AGP_IKKE_FORESPURT_ER_UGYLDIG =
        "Ugyldig arbeidsgiverperiode. Oppgi arbeidsgiverperiode bare ved nytt sykefravær der første fraværsdag er mer enn 16 dager etter forrige sykefraværsdag."
    const val UGYLDIG_AARSAK = "Ugyldig aarsak innsending"
    const val FORESPOERSEL_FORKASTET = "Forespoersel er trukket tilbake"
    const val UGYLDIG_REFERANSE = "Ugyldig referanse"
}
