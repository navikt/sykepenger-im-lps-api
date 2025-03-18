@file:UseSerializers(UuidSerializer::class, LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.inntektsmelding

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntekt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Refusjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.time.LocalDateTime
import java.util.UUID

// TODO: Innsending og InnsendtInntektsmelding deler mange felt - kan vi bruke bare en klasse?
//  Dersom begge klasser skal brukes, bør vi rydde opp i dette...
@Serializable
data class InnsendtInntektsmelding( // rename: bare Inntektsmelding..?
    val navReferanseId: UUID,
    val agp: Arbeidsgiverperiode?,
    val inntekt: Inntekt?,
    val refusjon: Refusjon?,
    val sykmeldtFnr: String,
    val aarsakInnsending: AarsakInnsending,
    val typeInnsending: InnsendingType,
    val innsendtTid: LocalDateTime,
    val versjon: Int,
    val arbeidsgiver: Arbeidsgiver,
    val avsender: Avsender,
    val status: InnsendingStatus,
    val statusMelding: String?,
)

@Serializable
data class InntektsmeldingSkjema( // Innsending slik APIet sender inn
    val navReferanseId: UUID,
    val agp: Arbeidsgiverperiode?,
    val inntekt: Inntekt?,
    val refusjon: Refusjon?,
    val sykmeldtFnr: String,
    val arbeidsgiver: Arbeidsgiver,
    val avsender: Avsender, // avsendersystem
) {
    fun valider(): Set<String> = SkjemaInntektsmelding(navReferanseId, arbeidsgiver.tlf, agp, inntekt, refusjon).valider()
}

enum class InnsendingType {
    FORESPURT,
    ARBEIDSGIVER_INITIERT,
    ;

    companion object {
        fun from(type: Inntektsmelding.Type): InnsendingType =
            when (type) {
                is Inntektsmelding.Type.Forespurt -> FORESPURT
                is Inntektsmelding.Type.Selvbestemt -> ARBEIDSGIVER_INITIERT
            }
    }
}

@Serializable
data class Arbeidsgiver(
    val orgnr: String, // Arbeidsgivers orgnr
    val tlf: String, // Arbeidsgiver
)

@Serializable
data class Avsender(
    val systemNavn: String,
    val systemVersjon: String,
)

@Serializable
data class InntektsmeldingRequest(
    val fnr: String? = null,
    val foresporselId: String? = null,
    val fraTid: LocalDateTime? = null,
    val tilTid: LocalDateTime? = null,
)

@Serializable
data class InntektsmeldingResponse(
    val antall: Int = 0,
    val inntektsmeldinger: List<InnsendtInntektsmelding>,
)
