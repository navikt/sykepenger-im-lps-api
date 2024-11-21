package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import no.nav.helsearbeidsgiver.db.Database
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.TestData.ARBEIDSGIVER_INITIERT_IM_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_BESVART
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.IM_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.SIMBA_PAYLOAD
import no.nav.helsearbeidsgiver.utils.TransactionalExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TransactionalExtension::class)
class ImKafkaConsumerTest {
    val db = Database.init()
    val inntektsmeldingRepository = InntektsmeldingRepository(db)
    val inntektsmeldingService = InntektsmeldingService(inntektsmeldingRepository)
    val mottakRepository = MottakRepository(db)
    val imKafkaConsumer = ImKafkaConsumer(inntektsmeldingService, mottakRepository)

    @Test
    fun kunLagreEventerSomMatcher() {
        // Test at kjente payloads ikke kræsjer:
        imKafkaConsumer.handleRecord(FORESPOERSEL_MOTTATT)
        imKafkaConsumer.handleRecord(FORESPOERSEL_BESVART)
        imKafkaConsumer.handleRecord(IM_MOTTATT)
        imKafkaConsumer.handleRecord(ARBEIDSGIVER_INITIERT_IM_MOTTATT)

        // Skal ikke lagre:
        imKafkaConsumer.handleRecord(SIMBA_PAYLOAD)
    }

    @Test
    fun duplikat() {
        imKafkaConsumer.handleRecord(FORESPOERSEL_MOTTATT)
        imKafkaConsumer.handleRecord(FORESPOERSEL_MOTTATT)
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
