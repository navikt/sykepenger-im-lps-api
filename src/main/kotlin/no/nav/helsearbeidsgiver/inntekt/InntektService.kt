package no.nav.helsearbeidsgiver.inntekt

import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class InntektService(
    private val inntektKlient: InntektKlient,
) {
    suspend fun hentInntekter(
        forespoersel: Forespoersel,
        inntektsdato: LocalDate,
    ): InntektMedGjennomsnittResponse {
        val fom = inntektsdato.minusMaaneder(3)
        val middle = inntektsdato.minusMaaneder(2)
        val tom = inntektsdato.minusMaaneder(1)

        val inntektPerOrgnrOgMaaned =
            hentInntektPerOrgnrOgMaaned(
                fnr = forespoersel.fnr,
                fom = fom,
                tom = tom,
                navReferanseId = forespoersel.navReferanseId,
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

    private suspend fun hentInntektPerOrgnrOgMaaned(
        fnr: String,
        fom: YearMonth,
        tom: YearMonth,
        navReferanseId: UUID,
    ): Map<String, Map<YearMonth, Double>> {
        val navConsumerId = "helsearbeidsgiver-im-lps-api"
        val callId = "$navConsumerId-$navReferanseId"

        sikkerLogger().info("Henter inntekt for $fnr i perioden $fom til $tom (callId: $callId).")

        return inntektKlient.hentInntektPerOrgnrOgMaaned(
            fnr = fnr,
            fom = fom,
            tom = tom,
            navConsumerId = navConsumerId,
            callId = callId,
        )
    }
}

private fun LocalDate.minusMaaneder(maanederTilbake: Long): YearMonth = toYearMonth().minusMonths(maanederTilbake)

private fun LocalDate.toYearMonth(): YearMonth = YearMonth.of(year, month)
