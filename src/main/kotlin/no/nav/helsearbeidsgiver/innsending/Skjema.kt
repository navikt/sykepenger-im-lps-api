@file:UseSerializers(UuidSerializer::class)

package no.nav.helsearbeidsgiver.innsending
import io.swagger.v3.oas.annotations.media.Schema
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Arbeidsgiverperiode
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntekt
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Refusjon
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.skjema.SkjemaInntektsmelding
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import java.util.UUID

@Serializable
/*
Skjema som representerer en IM innsendt via API
 */
@Schema(description = "Dette er en inntektsmelding blah") //TODO: Dette plukkes ikke opp av openapi / ktor plugin :(
data class Skjema( // TODO: Bedre navn -> Inntektsmelding..?
    val forespoersel_id: UUID,
    @Schema(description = "Telefonnummer til avsender", example = "33132323")
    val avsender_tlf: String,
    val arbeidsgiverperiode: Arbeidsgiverperiode?,
    val inntekt: Inntekt?,
    val refusjon: Refusjon?,
) {
    fun valider(): Set<String> = emptySet()

    fun tilSkjemaInntektsmelding(): SkjemaInntektsmelding =
        SkjemaInntektsmelding(
            forespoerselId = forespoersel_id,
            avsenderTlf = avsender_tlf,
            agp = arbeidsgiverperiode,
            inntekt = inntekt,
            refusjon = refusjon,
        )
}
