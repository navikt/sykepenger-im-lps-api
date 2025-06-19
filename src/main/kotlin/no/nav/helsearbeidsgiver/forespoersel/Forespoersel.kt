@file:UseSerializers(LocalDateSerializer::class, YearMonthSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.forespoersel

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.YearMonthSerializer
import java.util.UUID

@Serializable
data class Forespoersel(
    val navReferanseId: UUID,
    val orgnr: String,
    val fnr: String,
    val status: Status,
    val sykmeldingsperioder: List<Periode>,
    val egenmeldingsperioder: List<Periode>,
    val arbeidsgiverperiodePaakrevd: Boolean,
    val inntektPaakrevd: Boolean,
)

@Serializable
enum class Status {
    AKTIV,
    BESVART,
    FORKASTET,
}

enum class Type {
    KOMPLETT,
    BEGRENSET,
}

@Serializable
data class ForespoerselRequest(
    val fnr: String? = null,
    val navReferanseId: UUID? = null,
    val status: Status? = null,
)

@Serializable
data class ForespoerselResponse(
    val antall: Int,
    val forespoersler: List<Forespoersel>,
)

@Serializable
data class ForespurtData(
    val arbeidsgiverperiode: Arbeidsgiverperiode,
    val inntekt: Inntekt,
)

@Serializable
data class Arbeidsgiverperiode(
    val paakrevd: Boolean,
)

@Serializable
data class Inntekt(
    val paakrevd: Boolean,
)
