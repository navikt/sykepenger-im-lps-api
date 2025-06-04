@file:UseSerializers(LocalDateSerializer::class)

package no.nav.helsearbeidsgiver.sykmelding

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import java.time.LocalDate

@Serializable
data class SykmeldingFilterRequest(
    val fnr: String? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
)
