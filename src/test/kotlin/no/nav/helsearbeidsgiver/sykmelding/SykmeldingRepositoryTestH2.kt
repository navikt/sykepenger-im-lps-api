package no.nav.helsearbeidsgiver.sykmelding

import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.util.UUID

class SykmeldingRepositoryTestH2 {
    val db = Database.init()
    val sykmeldingRepository = SykmeldingRepository(db)

    @Test
    fun lagreSykmelding() {
        sykmeldingRepository.opprettSykmelding(sykmeldingMock())

        val lagretSykmelding = sykmeldingRepository.hentSykmeldingForSykmeldingId(UUID.fromString(sykmeldingMock().sykmelding.id))
        assertNotNull(lagretSykmelding, "Sykmelding ble ikke lagret i databasen")
    }
}
