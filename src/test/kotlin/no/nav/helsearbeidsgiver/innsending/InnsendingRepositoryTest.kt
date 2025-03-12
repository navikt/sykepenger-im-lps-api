package no.nav.helsearbeidsgiver.innsending

import no.nav.helsearbeidsgiver.config.DbConfig
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import no.nav.helsearbeidsgiver.utils.mockSkjemaInntektsmelding
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@ExtendWith(TransactionalExtension::class)
class InnsendingRepositoryTest {
    val db = DbConfig.init()
    val repository = InnsendingRepository(db)

    @Test
    fun `opprett innsending og innsendingen får status NY`() {
        val organisasjonsNr = "123456789"
        val lpsOrgnr = "987654321"
        val payload = mockSkjemaInntektsmelding()

        val result = repository.opprettInnsending(organisasjonsNr, lpsOrgnr, payload)
        assertNotNull(result)
        val innsending = repository.hentById(result)
        assertNotNull(innsending)
        assertEquals(organisasjonsNr, innsending.orgnr)
        assertEquals(lpsOrgnr, innsending.lps)
        assertEquals(payload, innsending.dokument)
        assertEquals(InnsendingStatus.MOTTATT, innsending.status)
    }

    @Test
    fun `hent alle innsendinger med status MOTTATT`() {
        val organisasjonsNr = "123456789"
        val lpsOrgnr = "987654321"
        val payload = mockSkjemaInntektsmelding()

        val result = repository.opprettInnsending(organisasjonsNr, lpsOrgnr, payload)
        assertNotNull(result)
        val innsendinger = repository.hentAlleInnsendingerByStatus(InnsendingStatus.MOTTATT)
        assertEquals(1, innsendinger.size)
        val innsending = innsendinger.first()
        assertEquals(organisasjonsNr, innsending.orgnr)
        assertEquals(lpsOrgnr, innsending.lps)
        assertEquals(payload, innsending.dokument)
    }
}
