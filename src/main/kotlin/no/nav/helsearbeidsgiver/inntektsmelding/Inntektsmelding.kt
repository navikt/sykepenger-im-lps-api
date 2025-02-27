@file:UseSerializers(LocalDateSerializer::class, LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.inntektsmelding

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class Inntektsmelding(
    //TODO: Fjern dokument, skal ikke eksponeres til LPS. merge properties fra dokument inn i denne klassen.
    val dokument: no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding,
    val orgnr: String,
    val fnr: String,
    val foresporsel_id: String?,
    val innsendt_tid: LocalDateTime,
    val mottatt_tid: LocalDateTime, //TODO: Er denne nødvendig?
)

@Serializable
data class InntektsmeldingRequest(
    val fnr: String? = null,
    val foresporsel_id: String? = null,
    val fra_dato: LocalDateTime? = null,
    val til_dato: LocalDateTime? = null,
)

@Serializable
data class InntektsmeldingResponse(
    val antall: Int = 0,
    val inntektsmeldinger: List<Inntektsmelding>,
)
