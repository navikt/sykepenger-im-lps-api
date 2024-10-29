@file:UseSerializers(LocalDateSerializer::class, YearMonthSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.forespoersel

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.YearMonthSerializer

@Serializable
data class Forespoersel(
    val forespoerselId: String,
    val orgnr: String,
    val fnr: String,
    val status: Status,
)

enum class Status {
    AKTIV,
    MOTTATT,
    FORKASTET,
}
