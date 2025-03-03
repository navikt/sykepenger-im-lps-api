package no.nav.helsearbeidsgiver.innsending

import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.utils.mockSkjemaInntektsmelding
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

class InnsendingRepositoryTest {
    val db = Database.init()
    val repository = InnsendingRepository(db)

    @Test
    fun opprettInnsending() {
        val organisasjonsNr = "123456789"
        val lpsOrgnr = "987654321"
        val payload = mockSkjemaInntektsmelding()

        val result = repository.opprettInnsending(organisasjonsNr, lpsOrgnr, payload)
        assertNotNull(result)
    }
}
