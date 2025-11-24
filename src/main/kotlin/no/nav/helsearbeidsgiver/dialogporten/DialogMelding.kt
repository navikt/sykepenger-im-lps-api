@file:UseSerializers(LocalDateSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.AarsakInnsending
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.util.UUID

@Serializable
sealed class DialogMelding

@Serializable
@SerialName("Sykmelding")
data class DialogSykmelding(
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
    val foedselsdato: LocalDate,
    val fulltNavn: String,
    val sykmeldingsperioder: List<Periode>,
) : DialogMelding()

@Serializable
@SerialName("Sykepengesoeknad")
data class DialogSykepengesoeknad(
    val soeknadId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
) : DialogMelding()

@Serializable
@SerialName("Inntektsmeldingsforespoersel")
data class DialogInntektsmeldingsforespoersel(
    val forespoerselId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
) : DialogMelding()

@Serializable
@SerialName("UtgaattInntektsmeldingForespoersel")
data class DialogUtgaattInntektsmeldingForespoersel(
    val forespoerselId: UUID,
    val sykmeldingId: UUID,
    val orgnr: String,
) : DialogMelding()

@Serializable
@SerialName("Inntektsmelding")
data class DialogInntektsmelding(
    val forespoerselId: UUID,
    val innsendingId: UUID,
    val sykmeldingId: UUID,
    val orgnr: String,
    val status: InnsendingStatus,
    val aarsakInnsending: AarsakInnsending,
    val kilde: Kilde?,
) : DialogMelding() {
    enum class Kilde {
        API,
        NAV_PORTAL,
    }
}
