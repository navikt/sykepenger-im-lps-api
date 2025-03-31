@file:UseSerializers(UuidSerializer::class)

package no.nav.helsearbeidsgiver.kafka.forespoersel.pri

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselDokument
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.util.UUID

@Serializable
data class PriMessage(
    @SerialName("notis") val notis: NotisType,
    @SerialName("forespoersel") val forespoersel: ForespoerselDokument? = null,
    @SerialName("forespoerselId") val forespoerselId: UUID? = null,
)
