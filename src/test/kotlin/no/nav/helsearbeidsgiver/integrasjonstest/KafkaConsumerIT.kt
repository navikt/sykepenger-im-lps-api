package no.nav.helsearbeidsgiver.integrasjonstest

import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.Env.getProperty
import no.nav.helsearbeidsgiver.Producer
import no.nav.helsearbeidsgiver.kafka.createKafkaConsumerConfig
import no.nav.helsearbeidsgiver.kafka.startKafkaConsumer
import no.nav.helsearbeidsgiver.kafka.sykmelding.SykmeldingTolker
import no.nav.helsearbeidsgiver.testcontainer.WithKafkaContainer
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.text.set

@WithKafkaContainer
class KafkaConsumerIT {
    @Test
    fun enkeltEksempel() =
        runTest {
            val sykmeldingTolker = mockk<SykmeldingTolker>()
            every { sykmeldingTolker.lesMelding(any()) } just runs

            val sykmeldingKafkaConsumer = KafkaConsumer<String, String>(createKafkaConsumerConfig("sm"))
            val topic = getProperty("kafkaConsumer.sykmelding.topic")

            val toggle = AtomicBoolean(true)

            val consumerJob =
                launch(Dispatchers.IO) {
                    startKafkaConsumer(
                        topic = topic,
                        consumer = sykmeldingKafkaConsumer,
                        meldingTolker = sykmeldingTolker,
                        { toggle.get() },
                    )
                }

            Producer.sendMelding(ProducerRecord(topic, "key", "{\"hello\":\"world 1\"}"))

            coVerify(timeout = 5000, exactly = 1) { sykmeldingTolker.lesMelding(any()) }

            runBlocking {
                toggle.set(false)
                Producer.sendMelding(ProducerRecord(topic, "key", "{\"hello\":\"world 2\"}"))
                kotlinx.coroutines.delay(10000)
                verify(exactly = 1) { sykmeldingTolker.lesMelding(any()) }
            }

            consumerJob.cancelAndJoin()
        }
}
