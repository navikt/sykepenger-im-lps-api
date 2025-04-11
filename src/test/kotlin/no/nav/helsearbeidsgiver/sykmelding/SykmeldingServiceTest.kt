package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.sendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertNull

@WithPostgresContainer
class SykmeldingServiceTest {
    private lateinit var db: Database
    private lateinit var sykmeldingService: SykmeldingService
    private lateinit var sykmeldingRepository: SykmeldingRepository

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        sykmeldingRepository = SykmeldingRepository(db)
        sykmeldingService = SykmeldingService(sykmeldingRepository)
    }

    @BeforeEach
    fun cleanDb() {
        transaction(db) { SykmeldingEntitet.deleteAll() }
    }

    @Test
    fun `lagreSykmelding skal lagre sykmelding`() {
        val sykmeldingKafkaMessage = sykmeldingMock().also { sykmeldingService.lagreSykmelding(it) }

        val lagretSykmelding = transaction(db) { SykmeldingEntitet.selectAll().firstOrNull()?.getOrNull(sendSykmeldingAivenKafkaMessage) }

        lagretSykmelding?.sykmelding shouldBe sykmeldingKafkaMessage.sykmelding
    }

    @Test
    fun `lagreSykmelding skal kaste SykmeldingOrgnrManglerException og ikke lagre når orgnr mangler`() {
        val eventUtenOrgnr = sykmeldingMock().event.copy(arbeidsgiver = null)
        val sykmeldingKafkaMessage = sykmeldingMock().copy(event = eventUtenOrgnr)

        assertThrows<SykmeldingOrgnrManglerException> { sykmeldingService.lagreSykmelding(sykmeldingKafkaMessage) }

        assertNull(transaction(db) { SykmeldingEntitet.selectAll().firstOrNull() })
    }

    @Test
    fun `lagreSykmelding skal kaste IllegalArgumentException og ikke lagre når sykmelding UUID er ugyldig`() {
        val sykmeldingMedUgyldigId = sykmeldingMock().sykmelding.copy(id = "ugyldig-uuid")
        val sykmeldingKafkaMessage = sykmeldingMock().copy(sykmelding = sykmeldingMedUgyldigId)

        assertThrows<IllegalArgumentException> { sykmeldingService.lagreSykmelding(sykmeldingKafkaMessage) }

        assertNull(transaction(db) { SykmeldingEntitet.selectAll().firstOrNull() })
    }

    @Test
    fun `hentSykmelding skal hente sykmelding`() {
        val sykmeldingKafkaMessage = sykmeldingMock().also { sykmeldingService.lagreSykmelding(it) }

        val id = UUID.fromString(sykmeldingKafkaMessage.sykmelding.id)
        val orgnr = sykmeldingKafkaMessage.event.arbeidsgiver!!.orgnummer

        sykmeldingService.hentSykmelding(id, orgnr) shouldBe sykmeldingKafkaMessage.toSykmeldingResponse().toArbeidsgiverSykmelding()
    }

    @Test
    fun `hentSykmelding skal returnere null når id ikke eksisterer`() {
        val sykmeldingKafkaMessage = sykmeldingMock().also { sykmeldingService.lagreSykmelding(it) }

        val feilId = UUID.randomUUID()

        assertNull(
            sykmeldingService.hentSykmelding(
                id = feilId,
                orgnr = sykmeldingKafkaMessage.event.arbeidsgiver!!.orgnummer,
            ),
        )
    }

    @Test
    fun `hentSykmelding skal returnere null når id eksisterer men orgnr ikke matcher`() {
        val sykmeldingKafkaMessage = sykmeldingMock().also { sykmeldingService.lagreSykmelding(it) }

        val id = UUID.fromString(sykmeldingKafkaMessage.sykmelding.id)
        val riktigOrgnr = sykmeldingKafkaMessage.event.arbeidsgiver!!.orgnummer
        assertNotNull(sykmeldingService.hentSykmelding(id, riktigOrgnr))

        val feilOrgnr = "feil-orgnr"
        assertNull(sykmeldingService.hentSykmelding(id, feilOrgnr))
    }
}
