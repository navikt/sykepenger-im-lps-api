package no.nav.helsearbeidsgiver.inntekt

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.config.configureAuthClient
import no.nav.helsearbeidsgiver.felles.auth.AuthClientIdentityProvider.AZURE_AD
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.utils.cache.LocalCache
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class InntektService(
    private val forespoerselRepository: ForespoerselRepository,
) {
    private val tokenGetter = configureAuthClient().tokenGetter(AZURE_AD, Env.getProperty("INNTEKT_SCOPE"))
    private val inntektKlient =
        InntektKlient(
            baseUrl = Env.getProperty("INNTEKT_URL"),
            cacheConfig = LocalCache.Config(entryDuration = 5.minutes, maxEntries = 10_000),
            getAccessToken = tokenGetter,
        )

    fun hentForespoersel(navReferanseId: UUID): Forespoersel? = forespoerselRepository.hentForespoersel(navReferanseId)

    fun hentInntekter(
        navReferanseId: UUID,
        inntektsdato: LocalDate,
    ): Map<String, Map<YearMonth, Double>> {
        val forespoersel =
            forespoerselRepository.hentForespoersel(navReferanseId)
                ?: throw IllegalArgumentException("Forespørsel med id $navReferanseId finnes ikke")

        val fom = inntektsdato.minusMaaneder(3)
        val tom = inntektsdato.minusMaaneder(1)

        return hentInntektPerOrgnrOgMaaned(
            fnr = Fnr(forespoersel.fnr),
            fom = fom,
            tom = tom,
            kontekstId = forespoersel.navReferanseId,
        )
    }

    private fun hentInntektPerOrgnrOgMaaned(
        fnr: Fnr,
        fom: YearMonth,
        tom: YearMonth,
        kontekstId: UUID,
    ): Map<String, Map<YearMonth, Double>> {
        val navConsumerId = "helsearbeidsgiver-im-inntekt"
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
