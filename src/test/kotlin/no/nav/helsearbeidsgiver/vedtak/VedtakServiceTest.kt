package no.nav.helsearbeidsgiver.vedtak

import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.helsearbeidsgiver.config.DatabaseConfig
import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.kafka.sis.Dokument
import no.nav.helsearbeidsgiver.testcontainer.WithPostgresContainer
import no.nav.helsearbeidsgiver.utils.TestData.vedtakMock
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.buildInntektsmelding
import no.nav.helsearbeidsgiver.utils.mockInntektsmeldingResponse
import no.nav.helsearbeidsgiver.utils.test.wrapper.genererGyldig
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

@WithPostgresContainer
class VedtakServiceTest {
    private val db: Database by lazy {
        DatabaseConfig(
            System.getProperty("database.url"),
            System.getProperty("database.username"),
            System.getProperty("database.password"),
        ).init()
    }
    private val vedtakRepository: VedtakRepository by lazy { VedtakRepository(db) }
    private val unleashFeatureToggles = mockk<UnleashFeatureToggles>()
    private val inntektsmeldingRepository = mockk<InntektsmeldingRepository>()
    private val dokumentkoblingService = mockk<DokumentkoblingService>()
    private val vedtakService: VedtakService by lazy {
        VedtakService(vedtakRepository, unleashFeatureToggles, inntektsmeldingRepository, dokumentkoblingService)
    }

    @BeforeEach
    fun clean() {
        transaction(db) { VedtakEntitet.deleteAll() }
        clearAllMocks()
    }

    @Test
    fun `lagreVedtak lagrer vedtaket når featuretoggle er på`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns true
        val vedtak = vedtakMock()

        vedtakService.lagreVedtak(vedtak)

        val lagretVedtak = transaction(db) { VedtakEntitet.selectAll().firstOrNull()?.getOrNull(VedtakEntitet.vedtak) }
        lagretVedtak shouldBe vedtak
    }

    @Test
    fun `lagreVedtak lagrer ikke vedtaket når featuretoggle er av`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns false

        vedtakService.lagreVedtak(vedtakMock())

        val lagretVedtak = transaction(db) { VedtakEntitet.selectAll().firstOrNull()?.getOrNull(VedtakEntitet.vedtak) }
        lagretVedtak shouldBe null

        verify(exactly = 0) {
            dokumentkoblingService.produserVedtakKobling(any(), any(), any(), Orgnr.genererGyldig())
        }
    }

    @Test
    fun `lagreVedtak produserer vedtakKobling med sykmelding- og inntektsmeldingId når begge finnes`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns true
        val sykmeldingId = UUID.randomUUID()
        val inntektsmeldingId = UUID.randomUUID()
        val vedtak =
            vedtakMock().copy(
                harArbeidsgiverOensketRefusjon = true,
                dokumenter =
                    listOf(
                        Dokument(sykmeldingId, Dokument.Type.Sykmelding),
                        Dokument(inntektsmeldingId, Dokument.Type.Inntektsmelding),
                    ),
            )
        every {
            dokumentkoblingService.produserVedtakKobling(any(), any(), any(), vedtak.organisasjonsnummer)
        } just Runs

        vedtakService.lagreVedtak(vedtak)

        val lagretVedtakId = transaction(db) { VedtakEntitet.selectAll().first().getOrNull(VedtakEntitet.vedtakId) }
        verify(exactly = 1) {
            dokumentkoblingService.produserVedtakKobling(
                vedtakId = lagretVedtakId!!,
                sykmeldingId = sykmeldingId,
                inntektsmeldingId = inntektsmeldingId,
                orgnr = vedtak.organisasjonsnummer,
            )
        }
    }

    @Test
    fun `lagreVedtak produserer ikke vedtakKobling når sykmelding eller inntektsmelding mangler`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns true
        val vedtak = vedtakMock().copy(dokumenter = listOf(Dokument(UUID.randomUUID(), Dokument.Type.Sykmelding)))

        vedtakService.lagreVedtak(vedtak)

        verify(exactly = 0) {
            dokumentkoblingService.produserVedtakKobling(any(), any(), any(), Orgnr.genererGyldig())
        }
    }

    @Test
    fun `lagreVedtak produserer ikke vedtakKobling når arbeidsgiver ikke har ønsket refusjon`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns true
        val vedtak =
            vedtakMock().copy(
                harArbeidsgiverOensketRefusjon = false,
                dokumenter =
                    listOf(
                        Dokument(UUID.randomUUID(), Dokument.Type.Sykmelding),
                        Dokument(UUID.randomUUID(), Dokument.Type.Inntektsmelding),
                    ),
            )

        vedtakService.lagreVedtak(vedtak)

        val lagretVedtak = transaction(db) { VedtakEntitet.selectAll().firstOrNull()?.getOrNull(VedtakEntitet.vedtak) }
        lagretVedtak shouldBe vedtak
        verify(exactly = 0) {
            dokumentkoblingService.produserVedtakKobling(any(), any(), any(), Orgnr.genererGyldig())
        }
    }

    @Test
    fun `lagreVedtak bruker første sykmelding når det finnes flere`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns true
        val foersteSykmeldingId = UUID.randomUUID()
        val inntektsmeldingId = UUID.randomUUID()
        val vedtak =
            vedtakMock().copy(
                harArbeidsgiverOensketRefusjon = true,
                dokumenter =
                    listOf(
                        Dokument(foersteSykmeldingId, Dokument.Type.Sykmelding),
                        Dokument(UUID.randomUUID(), Dokument.Type.Sykmelding),
                        Dokument(inntektsmeldingId, Dokument.Type.Inntektsmelding),
                    ),
            )
        every {
            dokumentkoblingService.produserVedtakKobling(any(), any(), any(), vedtak.organisasjonsnummer)
        } just Runs

        vedtakService.lagreVedtak(vedtak)

        verify(exactly = 1) {
            dokumentkoblingService.produserVedtakKobling(
                vedtakId = any(),
                sykmeldingId = foersteSykmeldingId,
                inntektsmeldingId = inntektsmeldingId,
                orgnr = vedtak.organisasjonsnummer,
            )
        }
    }

    @Test
    fun `lagreVedtak bruker nyeste inntektsmelding når det finnes flere`() {
        every { unleashFeatureToggles.skalLagreVedtakArbeidsgiver() } returns true
        val sykmeldingId = UUID.randomUUID()
        val eldsteInntektsmeldingId = UUID.randomUUID()
        val nyesteInntektsmeldingId = UUID.randomUUID()
        every { inntektsmeldingRepository.hentMedInnsendingId(eldsteInntektsmeldingId) } returns
            mockInntektsmeldingResponse(buildInntektsmelding(inntektsmeldingId = eldsteInntektsmeldingId))
                .copy(innsendtTid = LocalDateTime.now().minusDays(1))
        every { inntektsmeldingRepository.hentMedInnsendingId(nyesteInntektsmeldingId) } returns
            mockInntektsmeldingResponse(buildInntektsmelding(inntektsmeldingId = nyesteInntektsmeldingId))
                .copy(innsendtTid = LocalDateTime.now())
        val vedtak =
            vedtakMock().copy(
                harArbeidsgiverOensketRefusjon = true,
                dokumenter =
                    listOf(
                        Dokument(sykmeldingId, Dokument.Type.Sykmelding),
                        Dokument(eldsteInntektsmeldingId, Dokument.Type.Inntektsmelding),
                        Dokument(nyesteInntektsmeldingId, Dokument.Type.Inntektsmelding),
                    ),
            )
        every {
            dokumentkoblingService.produserVedtakKobling(any(), any(), any(), vedtak.organisasjonsnummer)
        } just Runs

        vedtakService.lagreVedtak(vedtak)

        verify(exactly = 1) {
            dokumentkoblingService.produserVedtakKobling(
                vedtakId = any(),
                sykmeldingId = sykmeldingId,
                inntektsmeldingId = nyesteInntektsmeldingId,
                orgnr = vedtak.organisasjonsnummer,
            )
        }
    }
}
