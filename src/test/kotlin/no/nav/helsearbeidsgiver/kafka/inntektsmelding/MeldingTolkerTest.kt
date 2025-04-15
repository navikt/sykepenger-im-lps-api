package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.serialization.SerializationException
import no.nav.hag.utils.bakgrunnsjobb.exposed.ExposedBakgrunnsjobRepository
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.config.Services
import no.nav.helsearbeidsgiver.config.Tolkere
import no.nav.helsearbeidsgiver.config.configureTolkere
import no.nav.helsearbeidsgiver.dialogporten.IDialogportenService
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.innsending.InnsendingService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingRepository
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.ARBEIDSGIVER_INITIERT_IM_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_BESVART
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.IM_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.SIMBA_PAYLOAD
import no.nav.helsearbeidsgiver.utils.TestData.SYKMELDING_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.TRENGER_FORESPOERSEL
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.test.json.removeJsonWhitespace
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID

@WithPostgresContainer
class MeldingTolkerTest {
    private lateinit var db: Database
    private lateinit var repositories: Repositories
    private lateinit var service: Services
    private lateinit var tolkere: Tolkere
    private lateinit var unleashFeatureToggles: UnleashFeatureToggles

    @BeforeAll
    fun setup() {
        db =
            DatabaseConfig(
                System.getProperty("database.url"),
                System.getProperty("database.username"),
                System.getProperty("database.password"),
            ).init()

        repositories =
            Repositories(
                inntektsmeldingRepository = InntektsmeldingRepository(db),
                forespoerselRepository = ForespoerselRepository(db),
                mottakRepository = mockk<MottakRepository>(relaxed = true),
                bakgrunnsjobbRepository = ExposedBakgrunnsjobRepository(db),
                sykmeldingRepository = mockk<SykmeldingRepository>(),
            )

        service =
            Services(
                forespoerselService = ForespoerselService(repositories.forespoerselRepository),
                inntektsmeldingService = InntektsmeldingService(repositories.inntektsmeldingRepository),
                innsendingService = mockk<InnsendingService>(),
                dialogportenService = mockk<IDialogportenService>(),
                sykmeldingService = mockk<SykmeldingService>(relaxed = true),
            )

        unleashFeatureToggles = mockk<UnleashFeatureToggles>(relaxed = true)

        tolkere = configureTolkere(service, repositories, unleashFeatureToggles)
    }

    @BeforeEach
    fun clearMocks() {
        clearAllMocks()
    }

    @Test
    fun kunLagreEventerSomMatcher() {
        // Test at kjente payloads ikke kræsjer:
        tolkere.forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)

        tolkere.forespoerselTolker.lesMelding(FORESPOERSEL_BESVART)
        tolkere.inntektsmeldingTolker.lesMelding(IM_MOTTATT)
        tolkere.inntektsmeldingTolker.lesMelding(ARBEIDSGIVER_INITIERT_IM_MOTTATT)

        // Skal ikke lagre:
        tolkere.inntektsmeldingTolker.lesMelding(SIMBA_PAYLOAD)
    }

    @Test
    fun `sykmeldingTolker deserialiserer, lagrer og oppretter dialog for gyldig sykmelding`() {
        every { service.sykmeldingService.lagreSykmelding(any()) } returns true

        every { unleashFeatureToggles.skalOppretteDialogVedMottattSykmelding() } returns true

        every { service.dialogportenService.opprettNyDialogMedSykmelding(any(), any(), any()) } returns
            UUID.randomUUID().toString()

        tolkere.sykmeldingTolker.lesMelding(SYKMELDING_MOTTATT)
        verifySequence {
            service.sykmeldingService.lagreSykmelding(any())
            service.dialogportenService.opprettNyDialogMedSykmelding(any(), any(), any())
        }
    }

    @Test
    fun `forespoerselTolker håndterer duplikater`() {
        assertDoesNotThrow {
            tolkere.forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)
            tolkere.forespoerselTolker.lesMelding(FORESPOERSEL_MOTTATT)
        }
    }

    @Test
    fun `trengerForespoersel-meldinger ignoreres uten å lagre til mottak`() {
        tolkere.forespoerselTolker.lesMelding(TRENGER_FORESPOERSEL)
        verify(exactly = 0) { repositories.mottakRepository.opprett(any()) }
    }

    @Test
    fun `SykmeldingTolker lesMelding kaster exception om arbeidsgiver er null`() {
        val mockJsonMedArbeidsgiverNull =
            SYKMELDING_MOTTATT.removeJsonWhitespace().replace(
                """
                "arbeidsgiver": {
                    "orgnummer": "315587336",
                    "juridiskOrgnummer": "928497704",
                    "orgNavn": "Lama utleiren"
                }""".removeJsonWhitespace(),
                """"arbeidsgiver":null""",
            )

        assertThrows<SerializationException> {
            tolkere.sykmeldingTolker.lesMelding(mockJsonMedArbeidsgiverNull)
        }
    }
}
