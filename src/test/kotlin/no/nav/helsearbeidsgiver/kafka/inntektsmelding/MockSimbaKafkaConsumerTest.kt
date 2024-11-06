package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.MottakRepository
import no.nav.helsearbeidsgiver.utils.TestData.ikkeAktuellPayload
import no.nav.helsearbeidsgiver.utils.TestData.payload
import no.nav.helsearbeidsgiver.utils.TestData.payload2
import no.nav.helsearbeidsgiver.utils.TestData.payload3
import no.nav.helsearbeidsgiver.utils.TestData.payload4
import org.junit.jupiter.api.Test

class MockSimbaKafkaConsumerTest {
    val inntektsmeldingRepository = mockk<InntektsmeldingRepository>()
    val forespoerselRepository = mockk<ForespoerselRepository>()
    val mottakRepository = mockk<MottakRepository>()

    val simbaKafkaConsumer = SimbaKafkaConsumer(inntektsmeldingRepository, forespoerselRepository, mottakRepository)

    @Test
    fun kunLagreEventerSomMatcher() {
        every { mottakRepository.opprett(any()) } returns 1
        every { inntektsmeldingRepository.opprett(any(), any(), any(), any()) } returns 1
        every { forespoerselRepository.settBesvart(any()) } returns 1
        every { forespoerselRepository.lagreForespoersel(any(), any(), any()) } just Runs
        // Test at kjente payloads ikke kræsjer:
        simbaKafkaConsumer.handleRecord(payload)
        simbaKafkaConsumer.handleRecord(payload2)
        simbaKafkaConsumer.handleRecord(payload3)
        simbaKafkaConsumer.handleRecord(payload4)
        verify(exactly = 4) { mottakRepository.opprett(any()) }

        // Skal ikke lagre:
        simbaKafkaConsumer.handleRecord(ikkeAktuellPayload)
        verify(exactly = 4) { mottakRepository.opprett(any()) }
    }

    @Test
    fun duplikat() {
        simbaKafkaConsumer.handleRecord(payload)
        simbaKafkaConsumer.handleRecord(payload)
    }

    @Test
    fun feilsituasjon() {
        /* 1. Kan ikke parse record til objekt
         *  Mulige årsaker:
         *  - Nytt format i simba (feil i lokal parser)
         *  - Feil format sendt fra simba (feil i Simba)
         *
         *  Løsning:
         *  - Hvis det er en melding vi tror vi skal ha (event matcher) - logg feil, stopp kafkaconsumer, ikke gå videre
         *  - Hvis bare jibberish: logg feil, lagre record i mottak (på DLQ), men fortsett å parse neste melding. Vurdere en threshold-nivå før man evt stopper?
         *
         *
         * 2. Feil ved lagring til db
         *    - feiler på mottak: gå i feil-mode
         *    - feiler på lagring objekt - rulle tilbake..
         *
         *
         * */
    }
}
