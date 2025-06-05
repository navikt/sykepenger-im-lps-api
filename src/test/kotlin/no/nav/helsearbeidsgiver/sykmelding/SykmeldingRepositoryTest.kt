package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingStatusKafkaEventDTO.ArbeidsgiverStatusDTO
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.DEFAULT_FNR
import no.nav.helsearbeidsgiver.utils.DEFAULT_ORG
import no.nav.helsearbeidsgiver.utils.TestData.sykmeldingMock
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

@WithPostgresContainer
class SykmeldingRepositoryTest {
    private lateinit var db: Database

    private lateinit var sykmeldingRepository: SykmeldingRepository

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()
        sykmeldingRepository = SykmeldingRepository(db)
    }

    @BeforeEach
    fun cleanDb() {
        transaction(db) { SykmeldingEntitet.deleteAll() }
    }

    @Test
    fun `lagreSykmelding skal lagre sykmelding`() {
        val sykmeldingKafkaMessage = sykmeldingMock()

        sykmeldingKafkaMessage.lagreSykmelding(sykmeldingRepository)

        val lagretSykmelding = sykmeldingRepository.hentSykmelding(UUID.fromString(sykmeldingKafkaMessage.sykmelding.id))

        lagretSykmelding?.sendSykmeldingAivenKafkaMessage shouldBe sykmeldingKafkaMessage
    }

    @Test
    fun `hentSykmelding skal hente sykmelding med id`() {
        val sykmeldinger = List(10) { UUID.randomUUID() }.map { id -> sykmeldingMock().medId(id.toString()) }

        sykmeldinger.forEach { it.lagreSykmelding(sykmeldingRepository) }

        val sykmeldingValgt = sykmeldinger[2]

        sykmeldingRepository.hentSykmelding(UUID.fromString(sykmeldingValgt.sykmelding.id))?.sendSykmeldingAivenKafkaMessage shouldBe
            sykmeldingValgt
    }

    @Test
    fun `hentSykmeldingerForOrgnr skal hente alle sykmeldinger med gitt orgnr`() {
        val orgnr = "987654321"
        val sykmeldinger = List(10) { UUID.randomUUID() }.map { id -> sykmeldingMock().medId(id.toString()) }
        val sykmeldingerMedGittOrgnr =
            List(10) { UUID.randomUUID() }
                .map { id -> sykmeldingMock().medId(id.toString()) }
                .map { it.medOrgnr(orgnr) }

        sykmeldinger.forEach { it.lagreSykmelding(sykmeldingRepository) }

        sykmeldingRepository.hentSykmeldinger(orgnr) shouldBe emptyList()

        sykmeldingerMedGittOrgnr.forEach { it.lagreSykmelding(sykmeldingRepository) }

        val sykmeldingerFraDatabase = sykmeldingRepository.hentSykmeldinger(orgnr)
        sykmeldingerFraDatabase.size shouldBe sykmeldingerMedGittOrgnr.size
        sykmeldingerFraDatabase.forEach { it.orgnr shouldBe orgnr }
    }

    @Test
    fun `filter for fnr på sykmeldinger skal gi korrekt filtrert resultat`() {
        val id = UUID.randomUUID()
        sykmeldingRepository.lagreSykmelding(
            id = id,
            fnr = DEFAULT_FNR,
            orgnr = DEFAULT_ORG,
            sykmelding = sykmeldingMock(),
            sykmeldtNavn = "Syyk i hodet",
        )

        sykmeldingRepository.hentSykmeldinger(DEFAULT_ORG, filter = SykmeldingFilterRequest(fnr = DEFAULT_FNR))[0].id shouldBe id.toString()

        sykmeldingRepository.hentSykmeldinger(DEFAULT_ORG, filter = SykmeldingFilterRequest(fnr = DEFAULT_FNR.reversed())) shouldBe
            emptyList()
    }

    @Test
    fun `datofilter på sykmeldinger skal gi korrekt filtrert resultat`() {
        val antallSykmeldinger = 3
        val sykmelding = sykmeldingMock()
        val mottatt = sykmelding.sykmelding.mottattTidspunkt
        val mottattDato = mottatt.toLocalDate()

        for (i in 0..<antallSykmeldinger) {
            sykmeldingRepository.lagreSykmelding(
                UUID.randomUUID(),
                fnr = DEFAULT_FNR,
                orgnr = DEFAULT_ORG,
                sykmelding = sykmeldingMock().medMottattTidspunkt(mottatt.plusDays(i.toLong())),
                sykmeldtNavn = "Syyk i hodet",
            )
        }

        for (i in 0..antallSykmeldinger) {
            val sykmeldingerFraOgMed =
                sykmeldingRepository.hentSykmeldinger(
                    DEFAULT_ORG,
                    filter =
                        SykmeldingFilterRequest(
                            fom = mottattDato.plusDays(i.toLong()),
                        ),
                )
            sykmeldingerFraOgMed.size shouldBe (antallSykmeldinger - i)

            val sykmeldingerTilOgMed =
                sykmeldingRepository.hentSykmeldinger(
                    DEFAULT_ORG,
                    filter =
                        SykmeldingFilterRequest(
                            tom = mottattDato.minusDays(1).plusDays(i.toLong()),
                        ),
                )
            sykmeldingerTilOgMed.size shouldBe i
        }

        val sykmeldingerFomOgTom =
            sykmeldingRepository.hentSykmeldinger(
                DEFAULT_ORG,
                filter =
                    SykmeldingFilterRequest(
                        fom = mottattDato,
                        tom = mottattDato.plusDays(1),
                    ),
            )
        sykmeldingerFomOgTom.size shouldBe 2
    }
}

private fun SendSykmeldingAivenKafkaMessage.medId(id: String) = copy(sykmelding = sykmelding.copy(id = id))

private fun SendSykmeldingAivenKafkaMessage.medOrgnr(orgnr: String) =
    copy(event = event.copy(arbeidsgiver = ArbeidsgiverStatusDTO(orgnr, "", "")))

private fun SendSykmeldingAivenKafkaMessage.medMottattTidspunkt(mottattTid: OffsetDateTime) =
    copy(sykmelding = sykmelding.copy(mottattTidspunkt = mottattTid))

private fun SendSykmeldingAivenKafkaMessage.lagreSykmelding(sykmeldingRepository: SykmeldingRepository) {
    sykmeldingRepository.lagreSykmelding(
        id = UUID.fromString(sykmelding.id),
        fnr = kafkaMetadata.fnr,
        orgnr = event.arbeidsgiver.orgnummer,
        sykmeldtNavn = "",
        sykmelding = this,
    )
}
