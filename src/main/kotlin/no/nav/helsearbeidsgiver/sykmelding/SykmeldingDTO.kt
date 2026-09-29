@file:UseSerializers(LocalDateTimeSerializer::class)

package no.nav.helsearbeidsgiver.sykmelding

import io.ktor.openapi.JsonSchema.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class SykmeldingDTO(
    @Format("int64")
    val loepenr: Long,
    val id: String,
    val fnr: String,
    val orgnr: String,
    val sendSykmeldingAivenKafkaMessage: SendSykmeldingAivenKafkaMessage,
    val sykmeldtNavn: String,
    val mottattAvNav: LocalDateTime,
    val sendtTilArbeidsgiver: LocalDateTime,
)
