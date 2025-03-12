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

// TODO: Merge Innsending og InnsendtInntektsmelding - en av dem beholdes
@Serializable
data class InnsendtInntektsmelding(
    val skjema: SkjemaInntektsmelding?, // TODO: Kan gjøre denne ikke-null - hvis vi sletter gamle data i databasen...!
    val orgnr: String,
    val fnr: String,
    val innsendt_tid: LocalDateTime,
    val aarsak_innsending: AarsakInnsending,
    val type_innsending: InnsendingType,
    val versjon: Int,
    val status: InnsendingStatus,
    val status_melding: String?,
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
    val foresporsel_id: String? = null,
    val fra_tid: LocalDateTime? = null,
    val til_tid: LocalDateTime? = null,
)

@Serializable
data class InntektsmeldingResponse(
    val antall: Int = 0,
    val inntektsmeldinger: List<InnsendtInntektsmelding>,
)
