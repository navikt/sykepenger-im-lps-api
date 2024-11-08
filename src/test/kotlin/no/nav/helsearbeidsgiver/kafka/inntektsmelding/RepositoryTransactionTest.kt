package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.mottak.ExposedMottak
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.TestData.forespoerselDokument
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import no.nav.helsearbeidsgiver.utils.readJsonFromResources
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertEquals

@ExtendWith(TransactionalExtension::class)
class RepositoryTransactionTest {
    val db = Database.init()
    val inntektsmeldingRepository = InntektsmeldingRepository(db)
    val forespoerselRepository = ForespoerselRepository(db)
    val mottakRepository = MottakRepository(db)

    @Test
    fun handleRecord() {
        val orgnr = "999999999"
        val fnr = "99999999999"
        // Simuler flere samtidige http-klientkall (hent/les) og diverse innkommende kafka-meldinger (opprett/skriv)
        runBlocking {
            val im = readJsonFromResources("im.json")
            val event =
                readJsonFromResources(
                    "inntektsmelding_distribuert.json",
                ).replace("%%%FORESPORSELID%%%", UUID.randomUUID().toString())
            for (i in 1..100) {
                launch {
                    inntektsmeldingRepository.hent(orgnr)
                    mottakRepository.opprett(ExposedMottak(event))
                    forespoerselRepository.hentForespoerslerForOrgnr(orgnr)
                }
                launch {
                    val forespoerselID = lagreInntektsmelding(im, orgnr, fnr)
                    forespoerselRepository.hentForespoerslerForOrgnr(orgnr)
                    forespoerselRepository.lagreForespoersel(forespoerselID, forespoerselDokument(orgnr, "123"))
                    forespoerselRepository.hentForespoerslerForOrgnr(orgnr)
                    forespoerselRepository.settBesvart(forespoerselID)
                    inntektsmeldingRepository.hent(orgnr)
                }
                launch {
                    forespoerselRepository.hentForespoerslerForOrgnr(orgnr)
                    inntektsmeldingRepository.hent(orgnr)
                }
            }
        }
        assertEquals(100, forespoerselRepository.hentForespoerslerForOrgnr(orgnr).count())
        assertEquals(100, inntektsmeldingRepository.hent(orgnr).count())
    }

    fun lagreInntektsmelding(
        im: String,
        orgnr: String,
        fnr: String,
    ): String {
        val forespoerselID = UUID.randomUUID().toString()
        val generert =
            im
                .replace("%%%FORESPORSELID%%%", forespoerselID)
                .replace("%%%ORGNR%%%", orgnr)
                .replace("%%%SYKMELDT%%%", fnr)
        inntektsmeldingRepository.opprett(generert, orgnr, fnr, LocalDateTime.now(), forespoerselID)
        return forespoerselID
    }
}
