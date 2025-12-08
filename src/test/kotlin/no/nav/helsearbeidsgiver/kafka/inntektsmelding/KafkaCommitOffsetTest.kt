package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import no.nav.helsearbeidsgiver.kafka.KafkaMonitor
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.kafka.forespoersel.ForespoerselTolker
import no.nav.helsearbeidsgiver.kafka.startKafkaConsumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.record.TimestampType
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.time.Duration.Companion.milliseconds

class KafkaCommitOffsetTest {
    @Test
    fun `ikke commit offset når noe feiler`() {
        val kafkaConsumer = mockk<KafkaConsumer<String, String>>(relaxed = true)

        val mockMeldingTolker = mockk<ForespoerselTolker>()
        val topicPartition = TopicPartition("test", 0)
        val mockRecord =
            ConsumerRecords(
                mapOf(
                    topicPartition to
                        listOf(
                            ConsumerRecord(
                                "test",
                                0,
                                0L,
                                Instant.now().toEpochMilli(),
                                TimestampType.CREATE_TIME,
                                8L,
                                3,
                                15,
                                "key",
                                "mocked message",
                            ),
                        ),
                ),
            )

        every { mockMeldingTolker.lesMelding(any()) } throws Exception("au")
        every { kafkaConsumer.subscribe(listOf("test")) } just runs
        val slot = slot<Set<TopicPartition>>()
        every { kafkaConsumer.pause(capture(slot)) } just runs
        every { kafkaConsumer.paused() } returns if (slot.isCaptured) slot.captured else emptySet()
        every { kafkaConsumer.poll(any<Duration>()) } returns mockRecord
        runTest(timeout = 500.milliseconds) {
            try {
                startKafkaConsumer("test", kafkaConsumer, mockMeldingTolker)
            } catch (e: Exception) {
            } finally {
                verify(exactly = 1) { kafkaConsumer.poll(any<Duration>()) }
                verify(exactly = 0) { kafkaConsumer.commitSync(any<MutableMap<TopicPartition, OffsetAndMetadata>>()) }
                KafkaMonitor.harFeil() shouldBe true
            }
        }
    }

    @Test
    fun `ikke les records med value=null`() {
        val kafkaConsumer = mockk<KafkaConsumer<String, String>>(relaxed = true)

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

        val slot = slot<Set<TopicPartition>>()
        every { kafkaConsumer.pause(capture(slot)) } just runs
        every { kafkaConsumer.paused() } returns if (slot.isCaptured) slot.captured else emptySet()

        every { kafkaConsumer.poll(any<Duration>()) } returnsMany listOf(mockRecord) andThenThrows
            Exception(
                "au",
            )

        every { kafkaConsumer.commitSync(any<MutableMap<TopicPartition, OffsetAndMetadata>>()) } just runs

        runTest(timeout = 500.milliseconds) {
            try {
                startKafkaConsumer("test", kafkaConsumer, forespoerselTolker)
            } catch (e: Exception) {
                // Ignorerer exception
            }
            verify(exactly = 0) { forespoerselTolker.lesMelding(any()) }
            verify(exactly = 1) { kafkaConsumer.commitSync(any<MutableMap<TopicPartition, OffsetAndMetadata>>()) }
        }
    }

    @Test
    fun `ikke commit offsets for feilende meldinger i batch`() {
        val kafkaConsumer = mockk<KafkaConsumer<String, String>>(relaxed = true)

        val meldingTolker = mockk<MeldingTolker>()
        every { meldingTolker.lesMelding("ok") } just runs
        every { meldingTolker.lesMelding("feil") } throws Exception("feil")
        val topicPartition = TopicPartition("test", 0)
        val mockRecord =
            ConsumerRecords(
                mapOf(
                    topicPartition to
                        listOf(
                            ConsumerRecord<String, String>(
                                "test",
                                0,
                                0L,
                                System.currentTimeMillis(),
                                TimestampType.CREATE_TIME,
                                1,
                                2,
                                3,
                                "key",
                                "ok",
                            ),
                            ConsumerRecord<String, String>(
                                "test",
                                0,
                                1L,
                                System.currentTimeMillis(),
                                TimestampType.CREATE_TIME,
                                1,
                                2,
                                3,
                                "key",
                                "feil",
                            ),
                        ),
                ),
            )
        KafkaMonitor.harFeil() shouldBe true
        every { kafkaConsumer.subscribe(listOf("test")) } just runs
        val slot = slot<Set<TopicPartition>>()
        every { kafkaConsumer.pause(capture(slot)) } just runs
        every { kafkaConsumer.paused() } returns if (slot.isCaptured) slot.captured else emptySet()

        every { kafkaConsumer.poll(any<Duration>()) } returnsMany listOf(mockRecord)
        every { kafkaConsumer.commitSync(any<MutableMap<TopicPartition, OffsetAndMetadata>>()) } just runs

        runTest(timeout = 500.milliseconds) {
            try {
                startKafkaConsumer("test", kafkaConsumer, meldingTolker)
            } catch (e: Exception) {
                // Ignorerer exception
            }
            verify(exactly = 2) { meldingTolker.lesMelding(any()) }
            verify(exactly = 1) { kafkaConsumer.commitSync(any<MutableMap<TopicPartition, OffsetAndMetadata>>()) }
        }
    }

    @Test
    fun `leader skal ikke begynne å konsumere`() {
        val kafkaConsumer = mockk<KafkaConsumer<String, String>>(relaxed = true)

        val meldingTolker = mockk<MeldingTolker>()
        runTest(timeout = 500.milliseconds) {
            startKafkaConsumer(
                topic = "test",
                consumer = kafkaConsumer,
                meldingTolker = meldingTolker,
                enabled = { true },
                isLeader = { true },
            )
            KafkaMonitor.harFeil() shouldBe false
            verify(exactly = 0) { kafkaConsumer.subscribe(listOf("test")) }
            verify(exactly = 0) { kafkaConsumer.close() }
            verify(exactly = 0) { meldingTolker.lesMelding(any()) }
            verify(exactly = 0) { kafkaConsumer.commitSync(any<MutableMap<TopicPartition, OffsetAndMetadata>>()) }
            verify(exactly = 0) { kafkaConsumer.poll(any<Duration>()) }
        }
    }
}
