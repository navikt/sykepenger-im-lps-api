package no.nav.helsearbeidsgiver.inntekt

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class InntektService(
    private val forespoerselRepository: ForespoerselRepository,
    private val inntektKlient: InntektKlient,
) {
    fun hentInntekter(
        navReferanseId: UUID,
        inntektsdato: LocalDate,
    ): InntektMedGjennomsnittResponse {
        val forespoersel =
            forespoerselRepository.hentForespoersel(navReferanseId)
                ?: throw IllegalArgumentException("Forespørsel med id $navReferanseId finnes ikke")

        val fom = inntektsdato.minusMaaneder(3)
        val middle = inntektsdato.minusMaaneder(2)
        val tom = inntektsdato.minusMaaneder(1)

        val inntektPerOrgnrOgMaaned =
            hentInntektPerOrgnrOgMaaned(
                fnr = Fnr(forespoersel.fnr),
                fom = fom,
                tom = tom,
                kontekstId = forespoersel.navReferanseId,
            )
        val inntektPerMaaned = inntektPerOrgnrOgMaaned[forespoersel.orgnr].orEmpty()

        val inntekt =
            listOf(fom, middle, tom)
                .associateWith { inntektPerMaaned[it] }
        val inntektMedGjennomsnittResponse = InntektMedGjennomsnittResponse.of(inntekt)
        sikkerLogger().info(
            "Hentet inntekt for forespørsel ${forespoersel.navReferanseId} for orgnr ${forespoersel.orgnr} i perioden $fom til $tom: $inntekt, gjennomsnitt: ${inntektMedGjennomsnittResponse.gjennomsnittAvMaaneder}",
        )
        return inntektMedGjennomsnittResponse
    }

    private fun hentInntektPerOrgnrOgMaaned(
        fnr: Fnr,
        fom: YearMonth,
        tom: YearMonth,
        kontekstId: UUID,
    ): Map<String, Map<YearMonth, Double>> {
        val navConsumerId = "helsearbeidsgiver-im-lps-api"
        val callId = "$navConsumerId-$kontekstId"

        sikkerLogger().info("Henter inntekt for $fnr i perioden $fom til $tom (callId: $callId).")

        return runBlocking {
            inntektKlient.hentInntektPerOrgnrOgMaaned(
                fnr = fnr.verdi,
                fom = fom,
                tom = tom,
                navConsumerId = navConsumerId,
                callId = callId,
            )
        }
    }
}

private fun LocalDate.minusMaaneder(maanederTilbake: Long): YearMonth = toYearMonth().minusMonths(maanederTilbake)

fun LocalDate.toYearMonth(): YearMonth = YearMonth.of(year, month)
