package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingEntitet.arbeidsgiverSykmelding
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class SykmeldingRepositoryTestH2 {
    val db = Database.init()
    val sykmeldingRepository = SykmeldingRepository(db)

    @Test
    fun lagreSykmelding() {
        val sykmeldingKafkaMessage = sykmeldingMock()

        val sykmeldingId = sykmeldingRepository.opprettSykmelding(sykmeldingKafkaMessage)

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
}
