package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.arbeidsgiverSykmelding
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertNull

class SykmeldingServiceTest {
    val db = DbConfig.init()
    val sykmeldingRepository = SykmeldingRepository(db)
    val sykmeldingService = SykmeldingService(sykmeldingRepository)

    @BeforeEach
    fun setup() {
        transaction(db) { SykmeldingEntitet.deleteAll() }
    }

    @Test
    fun `lagreSykmelding skal lagre sykmelding`() {
        val sykmeldingKafkaMessage = sykmeldingMock().also { sykmeldingService.lagreSykmelding(it) }

        val lagretSykmelding = transaction(db) { SykmeldingEntitet.selectAll().firstOrNull()?.getOrNull(arbeidsgiverSykmelding) }

        lagretSykmelding shouldBe sykmeldingKafkaMessage.sykmelding
    }

    @Test
    fun `lagreSykmelding skal _ikke_ lagre sykmelding dersom den sykmeldingsiden allerede finnes i databasen`() {
        val nowDate = LocalDate.now()
        val sykmeldinger =
            List(2) { nowDate }.mapIndexed { index, now ->
                sykmeldingMock().copyWithSyketilfelleStartDato(
                    now.minusDays(
                        index.toLong(),
                    ),
                )
            }

        sykmeldingService.lagreSykmelding(sykmeldinger[0])
        sykmeldingService.lagreSykmelding(sykmeldinger[1])

        val lagredeSykmeldinger =
            transaction(db) { SykmeldingEntitet.selectAll().mapNotNull { it.getOrNull(arbeidsgiverSykmelding) } }

        lagredeSykmeldinger.size shouldBe 1
        lagredeSykmeldinger[0].syketilfelleStartDato shouldBe sykmeldinger[0].sykmelding.syketilfelleStartDato
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

        sykmeldingService.hentSykmelding(id, orgnr)?.arbeidsgiverSykmelding shouldBe sykmeldingKafkaMessage.sykmelding
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

    private fun SendSykmeldingAivenKafkaMessage.copyWithSyketilfelleStartDato(syketilfelleStartDato: LocalDate) =
        copy(sykmelding = sykmelding.copy(syketilfelleStartDato = syketilfelleStartDato))
}
