package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.arbeidsgiverSykmelding
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertNull

class SykmeldingRepositoryTest {
    val db = DbConfig.init()
    val sykmeldingRepository = SykmeldingRepository(db)

    @Test
    fun `lagre sykmelding`() {
        val sykmeldingKafkaMessage = sykmeldingMock()

        val sykmeldingId = sykmeldingRepository.lagreSykmelding(sykmeldingKafkaMessage)

        val lagretSykmelding =
            transaction(db) {
                SykmeldingEntitet
                    .selectAll()
                    .where { SykmeldingEntitet.sykmeldingId eq sykmeldingId }
                    .firstOrNull()
                    ?.getOrNull(arbeidsgiverSykmelding)
            }

        assertNotNull(lagretSykmelding, "Sykmelding ble ikke lagret i databasen")
        lagretSykmelding shouldBe sykmeldingKafkaMessage.sykmelding
    }

    @Test
    fun `kast feil istedenfor å lagre en sykmelding som mangler orgnr`() {
        val sykmeldingKafkaMessage =
            sykmeldingMock().copy(
                event =
                    sykmeldingMock().event.copy(
                        arbeidsgiver = null,
                    ),
            )

        assertThrows<SykmeldingOrgnrManglerException> { sykmeldingRepository.lagreSykmelding(sykmeldingKafkaMessage) }

        val lagretSykmelding =
            transaction(db) {
                SykmeldingEntitet
                    .selectAll()
                    .where { SykmeldingEntitet.sykmeldingId eq UUID.fromString(sykmeldingKafkaMessage.sykmelding.id) }
                    .firstOrNull()
                    ?.getOrNull(arbeidsgiverSykmelding)
            }

        assertNull(lagretSykmelding, "Sykmelding ble lagret i databasen, selv om vi mangler orgnr")
    }
}
