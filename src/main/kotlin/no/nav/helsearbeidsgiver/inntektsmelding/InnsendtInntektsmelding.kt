@file:UseSerializers(LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.inntektsmelding

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime

// TODO: Innsending og InnsendtInntektsmelding deler mange felt - kan vi bruke bare en klasse?
//  Dersom begge klasser skal brukes, bør vi rydde opp i dette...
@Serializable
data class InnsendtInntektsmelding( // rename: Inntektsmelding..?
    val skjema: SkjemaInntektsmelding?, // TODO: Kan gjøre denne ikke-null - hvis vi sletter gamle data i databasen...!
    val orgnr: String,
    val fnr: String,
    val innsendtTid: LocalDateTime,
    val aarsakInnsending: AarsakInnsending,
    val typeInnsending: InnsendingType,
    val versjon: Int,
    val status: InnsendingStatus,
    val statusMelding: String?,
)

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
