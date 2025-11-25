@file:UseSerializers(LocalDateSerializer::class, UuidSerializer::class)

package no.nav.helsearbeidsgiver.dokumentkobling

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.utils.json.serializer.LocalDateSerializer
import no.nav.helsearbeidsgiver.utils.json.serializer.UuidSerializer
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.time.LocalDate
import java.util.UUID

@Serializable
sealed class Dokumentkobling

@Serializable
@SerialName("Sykmelding")
data class Sykmelding(
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
    val foedselsdato: LocalDate,
    val fulltNavn: String,
    val sykmeldingsperioder: List<Periode>,
) : Dokumentkobling()

@Serializable
@SerialName("Sykepengesoeknad")
data class Sykepengesoeknad(
    val soeknadId: UUID,
    val sykmeldingId: UUID,
    val orgnr: Orgnr,
) : Dokumentkobling()

@Serializable
@SerialName("VedtaksperiodeSoeknadKobling")
data class VedtaksperiodeSoeknadKobling(
    val vedtaksperiodeId: UUID,
    val soeknadId: UUID,
) : Dokumentkobling()

@Serializable
@SerialName("ForespoerselSendt")
data class ForespoerselSendt(
    val forespoerselKobling: ForespoerselKobling,
) : Dokumentkobling()

@Serializable
@SerialName("ForespoerselUtgaatt")
data class ForespoerselUtgaatt(
    val forespoerselKobling: ForespoerselKobling,
) : Dokumentkobling()

@Serializable
@SerialName("InntektsmeldingMottatt")
data class InntektsmeldingMottatt(
    val inntektsmeldingKobling: InntektsmeldingKobling,
) : Dokumentkobling()

@Serializable
@SerialName("InntektsmeldingAvvist")
data class InntektsmeldingAvvist(
    val inntektsmeldingKobling: InntektsmeldingKobling,
) : Dokumentkobling()

@Serializable
@SerialName("InntektsmeldingGodkjent")
data class InntektsmeldingGodkjent(
    val inntektsmeldingKobling: InntektsmeldingKobling,
) : Dokumentkobling()

@Serializable
abstract class ForespoerselKobling(
    val forespoerselId: UUID,
    val vedtaksperiodeId: UUID,
    val orgnr: String,
)

@Serializable
abstract class InntektsmeldingKobling(
    val innsendingId: UUID,
    val forespoerselId: UUID,
    val orgnr: String,
)
