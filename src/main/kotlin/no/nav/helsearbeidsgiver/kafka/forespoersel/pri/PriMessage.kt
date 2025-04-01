@file:UseSerializers(UuidSerializer::class)

package no.nav.helsearbeidsgiver.kafka.forespoersel.pri

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselDokument
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.util.UUID

@Serializable
data class PriMessage(
    val notis: NotisType,
    val forespoersel: ForespoerselDokument? = null,
    val forespoerselId: UUID? = null,
)
