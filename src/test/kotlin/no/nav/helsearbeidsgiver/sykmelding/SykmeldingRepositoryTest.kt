package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.arbeidsgiverSykmelding
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@WithPostgresContainer
class SykmeldingRepositoryTest {
    private lateinit var db: Database

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
    }

    @BeforeEach
    fun cleanDb() {
        transaction(db) { SykmeldingEntitet.deleteAll() }
    }

    @Test
    fun `lagreSykmelding skal lagre sykmelding`() {
        val sykmeldingKafkaMessage = sykmeldingMock()

        sykmeldingKafkaMessage.lagreSykmelding(sykmeldingRepository)

        val lagretSykmelding = transaction(db) { SykmeldingEntitet.selectAll().firstOrNull()?.getOrNull(arbeidsgiverSykmelding) }

        lagretSykmelding shouldBe sykmeldingKafkaMessage.sykmelding
    }

    @Test
    fun `hentSykmelding skal hente sykmelding med id`() {
        val sykmeldinger = List(10) { UUID.randomUUID() }.map { id -> sykmeldingMock().copyId(id.toString()) }

        sykmeldinger.forEach { it.lagreSykmelding(sykmeldingRepository) }

        val sykmeldingValgt = sykmeldinger[2].sykmelding

        sykmeldingRepository.hentSykmelding(UUID.fromString(sykmeldingValgt.id))?.arbeidsgiverSykmelding shouldBe sykmeldingValgt
    }
}

private fun SendSykmeldingAivenKafkaMessage.copyId(id: String) = copy(sykmelding = sykmelding.copy(id = id))

private fun SendSykmeldingAivenKafkaMessage.lagreSykmelding(sykmeldingRepository: SykmeldingRepository) {
    sykmeldingRepository.lagreSykmelding(
        id = UUID.fromString(sykmelding.id),
        fnr = kafkaMetadata.fnr,
        orgnr = event.arbeidsgiver!!.orgnummer,
        sykmelding = sykmelding,
    )
}
