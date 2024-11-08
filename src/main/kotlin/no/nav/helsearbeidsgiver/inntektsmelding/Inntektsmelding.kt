@file:UseSerializers(LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.inntektsmelding

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class Inntektsmelding(
    val dokument: String,
    val orgnr: String,
    val fnr: String,
    val foresporselid: String?,
    val innsendt: LocalDateTime,
    val mottattEvent: LocalDateTime,
)

@Serializable
data class InntektsmeldingRequest(
    val fnr: String? = null,
    val foresporselid: String? = null,
    val datoFra: LocalDateTime? = null,
    val datoTil: LocalDateTime? = null,
)

@Serializable
data class InntektsmeldingResponse(
    val antallInntektsmeldinger: Int = 0,
    val inntektsmeldinger: List<Inntektsmelding>,
)
