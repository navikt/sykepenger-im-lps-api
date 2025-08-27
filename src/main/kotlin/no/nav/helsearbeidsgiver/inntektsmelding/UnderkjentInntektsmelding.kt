@file:UseSerializers(UuidSerializer::class)

package no.nav.helsearbeidsgiver.inntektsmelding

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.innsending.InnsendingFeil
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.util.UUID

@Serializable
data class UnderkjentInntektsmelding(
    val inntektsmeldingId: UUID,
    val feilkode: InnsendingFeil.Feilkode,
)
