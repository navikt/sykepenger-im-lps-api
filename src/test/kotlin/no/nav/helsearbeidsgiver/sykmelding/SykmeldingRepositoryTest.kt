package no.nav.helsearbeidsgiver.sykmelding

import io.kotest.matchers.shouldBe
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.MAX_ANTALL_I_RESPONS
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

        val lagretSykmelding =
            sykmeldingRepository.hentSykmelding(UUID.fromString(sykmeldingKafkaMessage.sykmelding.id))

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
    fun `filter for fnr på sykmeldinger skal gi korrekt filtrert resultat`() {
        val id = UUID.randomUUID()
        sykmeldingRepository.lagreSykmelding(
            sykmeldingId = id,
            fnr = DEFAULT_FNR,
            orgnr = DEFAULT_ORG,
            sykmelding = sykmeldingMock(),
            sykmeldtNavn = "Syyk i hodet",
        )

        sykmeldingRepository
            .hentSykmeldinger(
                filter = SykmeldingFilter(orgnr = DEFAULT_ORG, fnr = DEFAULT_FNR),
            )[0]
            .id shouldBe id.toString()

        sykmeldingRepository.hentSykmeldinger(
            filter = SykmeldingFilter(orgnr = DEFAULT_ORG, fnr = DEFAULT_FNR.reversed()),
        ) shouldBe
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
                    filter =
                        SykmeldingFilter(
                            orgnr = DEFAULT_ORG,
                            fom = mottattDato.plusDays(i.toLong()),
                        ),
                )
            sykmeldingerFraOgMed.size shouldBe (antallSykmeldinger - i)

            val sykmeldingerTilOgMed =
                sykmeldingRepository.hentSykmeldinger(
                    filter =
                        SykmeldingFilter(
                            orgnr = DEFAULT_ORG,
                            tom = mottattDato.minusDays(1).plusDays(i.toLong()),
                        ),
                )
            sykmeldingerTilOgMed.size shouldBe i
        }

        val sykmeldingerFomOgTom =
            sykmeldingRepository.hentSykmeldinger(
                filter =
                    SykmeldingFilter(
                        orgnr = DEFAULT_ORG,
                        fom = mottattDato,
                        tom = mottattDato.plusDays(1),
                    ),
            )
        sykmeldingerFomOgTom.size shouldBe 2
    }

    @Test
    fun `repository skal begrense antall entiteter som returneres - kan max returnere maxLimit + 1`() {
        val orgnr = DEFAULT_ORG
        val sykmeldinger =
            List(MAX_ANTALL_I_RESPONS + 10) { UUID.randomUUID() }.map { id ->
                sykmeldingMock().medOrgnr(orgnr).medId(id.toString())
            }

        sykmeldinger.forEach { it.lagreSykmelding(sykmeldingRepository) }

        sykmeldingRepository.hentSykmeldinger(SykmeldingFilter(orgnr)).size shouldBe MAX_ANTALL_I_RESPONS + 1
    }

    @Test
    fun `hentSykmeldinger fraLoepenr skal returnere kun loepenr større enn oppgitt verdi`() {
        val sykmeldingID1 = UUID.randomUUID()
        val sykmeldingID2 = UUID.randomUUID()
        val sykmeldingID3 = UUID.randomUUID()
        sykmeldingRepository.lagreSykmelding(
            sykmeldingId = sykmeldingID1,
            fnr = DEFAULT_FNR,
            orgnr = DEFAULT_ORG,
            sykmelding = sykmeldingMock().medId(sykmeldingID1.toString()),
            sykmeldtNavn = "Syk i hodet",
        )
        sykmeldingRepository.lagreSykmelding(
            sykmeldingId = sykmeldingID2,
            fnr = DEFAULT_FNR,
            orgnr = DEFAULT_ORG,
            sykmelding = sykmeldingMock().medId(sykmeldingID2.toString()),
            sykmeldtNavn = "Syk i hodet",
        )
        sykmeldingRepository.lagreSykmelding(
            sykmeldingId = sykmeldingID3,
            fnr = DEFAULT_FNR,
            orgnr = DEFAULT_ORG,
            sykmelding = sykmeldingMock().medId(sykmeldingID3.toString()),
            sykmeldtNavn = "Syk i hodet",
        )
        val sykmelding1Loepenr =
            sykmeldingRepository
                .hentSykmeldinger(SykmeldingFilter(orgnr = DEFAULT_ORG))
                .first { it.id == sykmeldingID1.toString() }
                .loepenr
        sykmeldingRepository.hentSykmeldinger(SykmeldingFilter(orgnr = DEFAULT_ORG, fraLoepenr = sykmelding1Loepenr)).size shouldBe 2
    }
}

fun SendSykmeldingAivenKafkaMessage.medId(id: String) = copy(sykmelding = sykmelding.copy(id = id))

fun SendSykmeldingAivenKafkaMessage.medOrgnr(orgnr: String) = copy(event = event.copy(arbeidsgiver = ArbeidsgiverStatusDTO(orgnr, "", "")))

private fun SendSykmeldingAivenKafkaMessage.medMottattTidspunkt(mottattTid: OffsetDateTime) =
    copy(sykmelding = sykmelding.copy(mottattTidspunkt = mottattTid))

private fun SendSykmeldingAivenKafkaMessage.lagreSykmelding(sykmeldingRepository: SykmeldingRepository) {
    sykmeldingRepository.lagreSykmelding(
        sykmeldingId = UUID.fromString(sykmelding.id),
        fnr = kafkaMetadata.fnr,
        orgnr = event.arbeidsgiver.orgnummer,
        sykmeldtNavn = "",
        sykmelding = this,
    )
}
