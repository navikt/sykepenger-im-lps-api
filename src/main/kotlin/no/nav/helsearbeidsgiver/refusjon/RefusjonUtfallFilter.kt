@file:UseSerializers(LocalDateSerializer::class)

package no.nav.helsearbeidsgiver.refusjon

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr.Companion.erGyldig
import java.time.LocalDate

@Serializable
data class RefusjonUtfallFilter(
    val orgnr: String,
    val fnr: String? = null,
    val fom: LocalDate? = null,
    val tom: LocalDate? = null,
) {
    init {
        require(erGyldig(orgnr)) { "ikke et gyldig orgnr" }
        fom?.year?.let { require(it >= 0) { "fom kan ikke være mindre enn år 0" } }
        tom?.year?.let { require(it <= 9999) { "tom kan ikke være etter år 9999" } }
    }
}
