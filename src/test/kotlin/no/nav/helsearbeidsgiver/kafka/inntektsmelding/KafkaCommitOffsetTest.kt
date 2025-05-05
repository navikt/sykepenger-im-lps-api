package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.kafka.startKafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class KafkaCommitOffsetTest {
    @Test
    fun `ikke commit offset når noe feiler`() {
        val kafkaConsumer = mockk<KafkaConsumer<String, String>>()

        val mockMeldingTolker = mockk<ForespoerselTolker>()
        val topicPartition = TopicPartition("test", 0)
        val mockRecord =
            ConsumerRecords(mapOf(topicPartition to listOf(ConsumerRecord("test", 0, 0L, "key", "mocked message"))))

        every { mockMeldingTolker.lesMelding(any()) } throws Exception("au")
        every { kafkaConsumer.subscribe(listOf("test")) } just runs
        every { kafkaConsumer.poll(any<Duration>()) } returns mockRecord
        runTest(timeout = 500.milliseconds) {
            try {
                startKafkaConsumer("test", kafkaConsumer, mockMeldingTolker)
            } catch (e: Exception) {
            } finally {
                verify(exactly = 1) { kafkaConsumer.poll(any<Duration>()) }
                verify(exactly = 0) { kafkaConsumer.commitSync() }
            }
        }
    }

    @Test
    fun `ikke les records med value=null`() {
        val kafkaConsumer = mockk<KafkaConsumer<String, String>>()

        val forespoerselTolker =
            spyk(
                ForespoerselTolker(
                    mockk(),
                    mockk(),
                ),
            )
        val topicPartition = TopicPartition("test", 0)
        val mockRecord =
            ConsumerRecords(
                mapOf(
                    topicPartition to
                        listOf(
                            ConsumerRecord<String, String>("", 0, 0L, "key", null),
                        ),
                ),
            )

        every { kafkaConsumer.subscribe(listOf("test")) } just runs

        every { kafkaConsumer.poll(any<Duration>()) } returnsMany listOf(mockRecord) andThenThrows
            Exception(
                "au",
            )

        every { kafkaConsumer.commitSync() } just runs

        runTest(timeout = 500.milliseconds) {
            try {
                startKafkaConsumer("test", kafkaConsumer, forespoerselTolker)
            } catch (e: Exception) {
                // Ignorerer exception
            }
            verify(exactly = 0) { forespoerselTolker.lesMelding(any()) }
            verify(exactly = 1) { kafkaConsumer.commitSync() }
        }
    }
}
