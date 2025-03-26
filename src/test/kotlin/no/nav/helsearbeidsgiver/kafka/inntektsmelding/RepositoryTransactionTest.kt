package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.mottak.ExposedMottak
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData.forespoerselDokument
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.readJsonFromResources
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(TransactionalExtension::class)
class RepositoryTransactionTest {
    val db = DbConfig.init()
    val inntektsmeldingRepository = InntektsmeldingRepository(db)
    val forespoerselRepository = ForespoerselRepository(db)
    val mottakRepository = MottakRepository(db)

    @Test
    fun handleRecord() {
        // Simuler flere samtidige http-klientkall (hent/les) og diverse innkommende kafka-meldinger (opprett/skriv)
        runBlocking {
            val event =
                readJsonFromResources(
                    "inntektsmelding_distribuert.json",
                ).replace("%%%FORESPORSELID%%%", UUID.randomUUID().toString())
            for (i in 1..100) {
                launch {
                    inntektsmeldingRepository.hent(DEFAULT_ORG)
                    mottakRepository.opprett(ExposedMottak(event))
                    forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
                }
                launch {
                    val forespoerselID = lagreInntektsmelding()
                    forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
                    forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR))
                    forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
                    forespoerselRepository.settBesvart(forespoerselID)
                    inntektsmeldingRepository.hent(DEFAULT_ORG)
                }
                launch {
                    forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
                    inntektsmeldingRepository.hent(DEFAULT_ORG)
                }
            }
        }
        assertEquals(100, forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG).count())
        assertEquals(100, inntektsmeldingRepository.hent(DEFAULT_ORG).count())
    }

    fun lagreInntektsmelding(): String {
        val forespoerselID = UUID.randomUUID().toString()
        val generert = buildInntektsmelding(forespoerselID)
        inntektsmeldingRepository.opprettInntektsmelding(generert)
        return forespoerselID
    }
}
