package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.config.configureRepositories
import no.nav.helsearbeidsgiver.forespoersel.Status
import no.nav.helsearbeidsgiver.mottak.ExposedMottak
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData.forespoerselDokument
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.readJsonFromResources
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals

@WithPostgresContainer
class RepositoryTransactionTest {
    private lateinit var db: Database
    private lateinit var repositories: Repositories

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        repositories = configureRepositories(db)
    }

    @Test
    fun handleRecord() {
        // Simuler flere samtidige http-klientkall (hent/les) og diverse innkommende kafka-meldinger (opprett/skriv)
        runBlocking {
            val event =
                readJsonFromResources(
                    "json/inntektsmelding_distribuert.json",
                ).replace("%%%FORESPORSELID%%%", UUID.randomUUID().toString())
            for (i in 1..100) {
                launch {
                    repositories.inntektsmeldingRepository.hent(DEFAULT_ORG)
                    repositories.mottakRepository.opprett(ExposedMottak(event))
                    repositories.forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
                }
                launch {
                    val forespoerselID = lagreInntektsmelding()
                    repositories.forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
                    repositories.forespoerselRepository.lagreForespoersel(
                        forespoerselDokument(DEFAULT_ORG, DEFAULT_FNR),
                        eksponertForespoerselId = forespoerselID,
                    )
                    repositories.forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
                    repositories.forespoerselRepository.oppdaterStatus(forespoerselID, Status.BESVART)
                    repositories.inntektsmeldingRepository.hent(DEFAULT_ORG)
                }
                launch {
                    repositories.forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG)
                    repositories.inntektsmeldingRepository.hent(DEFAULT_ORG)
                }
            }
        }
        assertEquals(100, repositories.forespoerselRepository.hentForespoerslerForOrgnr(DEFAULT_ORG).count())
        assertEquals(100, repositories.inntektsmeldingRepository.hent(DEFAULT_ORG).count())
    }

    fun lagreInntektsmelding(): UUID {
        val forespoerselID = UUID.randomUUID()
        val generert = buildInntektsmelding(forespoerselID)
        repositories.inntektsmeldingRepository.opprettInntektsmelding(generert)
        return forespoerselID
    }
}
