package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
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
import no.nav.helsearbeidsgiver.dialogporten.DialogportenService
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselRepository
import no.nav.helsearbeidsgiver.forespoersel.ForespoerselService
import no.nav.helsearbeidsgiver.innsending.InnsendingService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.pdl.PdlService
import no.nav.helsearbeidsgiver.pdl.domene.FullPerson
import no.nav.helsearbeidsgiver.pdl.domene.PersonNavn
import no.nav.helsearbeidsgiver.soknad.SoeknadRepository
import no.nav.helsearbeidsgiver.soknad.SoeknadService
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingRepository
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.ARBEIDSGIVER_INITIERT_IM_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_BESVART
import no.nav.helsearbeidsgiver.utils.TestData.FORESPOERSEL_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.IM_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.SIMBA_PAYLOAD
import no.nav.helsearbeidsgiver.utils.TestData.SYKEPENGESOKNAD
import no.nav.helsearbeidsgiver.utils.TestData.SYKMELDING_MOTTATT
import no.nav.helsearbeidsgiver.utils.TestData.TRENGER_FORESPOERSEL
import no.nav.helsearbeidsgiver.utils.buildJournalfoertInntektsmelding
import no.nav.helsearbeidsgiver.utils.test.json.removeJsonWhitespace
import org.jetbrains.exposed.sql.Database
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@WithPostgresContainer
class MeldingTolkerTest {
    private lateinit var db: Database
    private lateinit var repositories: Repositories
    private lateinit var service: Services
    private lateinit var tolkere: Tolkere

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
                soeknadRepository = mockk<SoeknadRepository>(),
            )

        service =
            Services(
                forespoerselService = ForespoerselService(repositories.forespoerselRepository),
                inntektsmeldingService = InntektsmeldingService(repositories.inntektsmeldingRepository),
                innsendingService = mockk<InnsendingService>(),
                dialogportenService = mockk<DialogportenService>(),
                sykmeldingService = mockk<SykmeldingService>(relaxed = true),
                pdlService = mockk<PdlService>(),
                soeknadService = mockk<SoeknadService>(),
            )

        tolkere = configureTolkere(service, repositories)
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
        tolkere.inntektsmeldingTolker.lesMelding(buildJournalfoertInntektsmelding())
        tolkere.inntektsmeldingTolker.lesMelding(ARBEIDSGIVER_INITIERT_IM_MOTTATT)

        tolkere.inntektsmeldingTolker.lesMelding(IM_MOTTATT)
    }

    @Test
    fun `ugyldig inntektsmelding kaster exception`() {
        assertThrows<SerializationException> {
            tolkere.inntektsmeldingTolker.lesMelding(SIMBA_PAYLOAD)
        }
    }

    @Test
    fun `sykmeldingTolker deserialiserer, lagrer og oppretter dialog for gyldig sykmelding`() {
        every { service.sykmeldingService.lagreSykmelding(any(), any(), any()) } returns true
        coEvery { service.pdlService.hentFullPerson(any()) } returns
            FullPerson(
                navn = PersonNavn(fornavn = "Testfrans", mellomnavn = null, etternavn = "Testesen"),
                foedselsdato = LocalDate.now().minusYears(1),
            )

        every { service.dialogportenService.opprettNyDialogMedSykmelding(any()) } just Runs

        tolkere.sykmeldingTolker.lesMelding(SYKMELDING_MOTTATT)
        verifySequence {
            service.sykmeldingService.lagreSykmelding(any(), any(), any())
            service.dialogportenService.opprettNyDialogMedSykmelding(any())
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
                """"arbeidsgiver":\{[^}]*}""".toRegex(),
                """"arbeidsgiver":null""",
            )

        assertThrows<SerializationException> {
            tolkere.sykmeldingTolker.lesMelding(mockJsonMedArbeidsgiverNull)
        }
    }

    @Test
    fun `SoknadTolker lesMelding klarer å deserialisere soknad`() {
        every { service.soeknadService.behandleSoeknad(any()) } just Runs
        val soknadJson =
            SYKEPENGESOKNAD.removeJsonWhitespace()
        tolkere.soknadTolker.lesMelding(soknadJson)

        verify(exactly = 1) { service.soeknadService.behandleSoeknad(any()) }
    }

    @Test
    fun `SoknadTolker lesMelding kaster exception om fnr er null`() {
        val mockJsonMedArbeidsgiverNull =
            SYKEPENGESOKNAD.removeJsonWhitespace().replace(
                """"fnr":"05449412615"""",
                """"fnr":null""",
            )

        assertThrows<SerializationException> {
            tolkere.soknadTolker.lesMelding(mockJsonMedArbeidsgiverNull)
        }
        verify(exactly = 0) { service.soeknadService.behandleSoeknad(any()) }
    }
}
