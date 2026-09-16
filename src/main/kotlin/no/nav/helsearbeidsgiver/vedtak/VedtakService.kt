package no.nav.helsearbeidsgiver.vedtak

import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingRepository
import no.nav.helsearbeidsgiver.kafka.sis.Dokument
import no.nav.helsearbeidsgiver.kafka.sis.VedtakArbeidsgiverMelding
import no.nav.helsearbeidsgiver.soeknad.SoeknadRepository
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingRepository
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.kapitaliserNavn
import no.nav.helsearbeidsgiver.utils.log.logger
import java.util.UUID

class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val unleashFeatureToggles: UnleashFeatureToggles,
    private val inntektsmeldingRepository: InntektsmeldingRepository,
    private val dokumentkoblingService: DokumentkoblingService,
    private val sykmeldingRepository: SykmeldingRepository,
    private val soeknadRepository: SoeknadRepository,
) {
    private val logger = logger()

    fun hentVedtak(vedtakId: UUID): VedtakForPdf? {
        val rad = vedtakRepository.hentVedtak(vedtakId) ?: return null

        val vedtaksdokumenter = rad.vedtak.dokumenter
        val vedtaksperiodeId = rad.vedtak.vedtaksperiodeId

        val sykmeldingId = finnSykmeldingId(dokumenter = vedtaksdokumenter, vedtaksperiodeId = vedtaksperiodeId)
        val sykmeldtNavn = sykmeldingId?.let { hentSykmeldtNavn(sykmeldingId = it, vedtaksperiodeId = vedtaksperiodeId) }

        val soeknadId = finnSoeknadId(dokumenter = vedtaksdokumenter, vedtaksperiodeId = vedtaksperiodeId)
        val virksomhetsnavn = soeknadId?.let { hentVirksomhetsnavn(soeknadId = it, vedtaksperiodeId = vedtaksperiodeId) }

        return VedtakForPdf(
            vedtakId = rad.vedtakId,
            orgnr = rad.orgnr,
            fom = rad.vedtak.fom,
            tom = rad.vedtak.tom,
            sykepengegrunnlag = rad.vedtak.sykepengegrunnlag,
            vedtaksUtfallTilArbeidsgiver = rad.vedtak.vedtaksUtfallTilArbeidsgiver,
            vedtakFattetTidspunkt = rad.vedtak.vedtakFattetTidspunkt,
            sykmeldtNavn = sykmeldtNavn,
            virksomhetsnavn = virksomhetsnavn,
        )
    }

    fun lagreVedtak(vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding) {
        if (unleashFeatureToggles.skalLagreVedtakArbeidsgiver()) {
            val vedtakId = UUID.randomUUID()
            vedtakRepository.lagreVedtak(
                vedtakId = vedtakId,
                vedtaksperiodeId = vedtakArbeidsgiverMelding.vedtaksperiodeId,
                fnr = vedtakArbeidsgiverMelding.foedselsnummer,
                orgnr = vedtakArbeidsgiverMelding.organisasjonsnummer,
                vedtak = vedtakArbeidsgiverMelding,
            )

            if (vedtakArbeidsgiverMelding.harArbeidsgiverOensketRefusjon) {
                produserVedtakKobling(vedtakId, vedtakArbeidsgiverMelding)
            } else {
                logger.info(
                    "Sender _ikke_ melding på helsearbeidsgiver.dokument-kobling for vedtak med vedtaksperiodeId " +
                        "${vedtakArbeidsgiverMelding.vedtaksperiodeId}, fordi arbeidsgiver ikke har ønsket refusjon.",
                )
            }
        } else {
            logger.info(
                "Lagrer _ikke_ vedtak for vedtaksperiodeId ${vedtakArbeidsgiverMelding.vedtaksperiodeId} fordi " +
                    "featuretoggle lagre-vedtak-arbeidsgiver er skrudd av.",
            )
        }
    }

    private fun produserVedtakKobling(
        vedtakId: UUID,
        vedtakArbeidsgiverMelding: VedtakArbeidsgiverMelding,
    ) {
        val vedtaksperiodeId = vedtakArbeidsgiverMelding.vedtaksperiodeId
        val sykmeldingId = finnSykmeldingId(vedtakArbeidsgiverMelding.dokumenter, vedtaksperiodeId)
        val inntektsmeldingId = finnInntektsmeldingId(vedtakArbeidsgiverMelding.dokumenter, vedtaksperiodeId)

        if (sykmeldingId == null || inntektsmeldingId == null) {
            logger.warn(
                "Mangler sykmeldingId og/eller inntektsmeldingId for vedtak med vedtaksperiodeId " +
                    "$vedtaksperiodeId (sykmeldingId=$sykmeldingId, inntektsmeldingId=$inntektsmeldingId), " +
                    "sender ikke melding på helsearbeidsgiver.dokument-kobling.",
            )
            return
        }

        dokumentkoblingService.produserVedtakKobling(
            vedtakId = vedtakId,
            sykmeldingId = sykmeldingId,
            inntektsmeldingId = inntektsmeldingId,
            orgnr = vedtakArbeidsgiverMelding.organisasjonsnummer,
        )
    }

    private fun hentSykmeldtNavn(
        sykmeldingId: UUID,
        vedtaksperiodeId: UUID,
    ): String? {
        val sykmeldtNavn = sykmeldingRepository.hentSykmelding(sykmeldingId)?.sykmeldtNavn
        if (sykmeldtNavn == null) {
            logger.error(
                "Fant ikke sykmelding med sykmeldingId $sykmeldingId for vedtak med vedtaksperiodeId $vedtaksperiodeId, " +
                    "og klarer derfor ikke hente sykmeldt sitt navn til vedtak-pdf.",
            )
        }
        return sykmeldtNavn?.kapitaliserNavn()
    }

    private fun hentVirksomhetsnavn(
        soeknadId: UUID,
        vedtaksperiodeId: UUID,
    ): String? {
        val virksomhetsnavn =
            soeknadRepository
                .hentSoeknad(soeknadId)
                ?.sykepengeSoeknadKafkaMelding
                ?.arbeidsgiver
                ?.navn
        if (virksomhetsnavn == null) {
            logger.error(
                "Fant ikke søknad med søknadId $soeknadId for vedtak med vedtaksperiodeId $vedtaksperiodeId, " +
                    "og klarer derfor ikke hente virksomhetsnavn til vedtak-pdf.",
            )
        }
        return virksomhetsnavn
    }

    private fun finnSykmeldingId(
        dokumenter: List<Dokument>,
        vedtaksperiodeId: UUID,
    ): UUID? {
        val sykmeldinger = dokumenter.filter { it.type == Dokument.Type.Sykmelding }
        if (sykmeldinger.size > 1) {
            logger.warn(
                "Fant ${sykmeldinger.size} sykmeldinger i vedtakmelding med vedtaksperiodeId $vedtaksperiodeId. " +
                    "Bruker den første.",
            )
        }
        return sykmeldinger.firstOrNull()?.dokumentId
    }

    private fun finnSoeknadId(
        dokumenter: List<Dokument>,
        vedtaksperiodeId: UUID,
    ): UUID? {
        val soeknader = dokumenter.filter { it.type == Dokument.Type.Soeknad }
        if (soeknader.size > 1) {
            logger.warn(
                "Fant ${soeknader.size} søknader i vedtakmelding med vedtaksperiodeId $vedtaksperiodeId, " +
                    "Bruker den første.",
            )
        }
        return soeknader.firstOrNull()?.dokumentId
    }

    private fun finnInntektsmeldingId(
        dokumenter: List<Dokument>,
        vedtaksperiodeId: UUID,
    ): UUID? {
        val inntektsmeldinger = dokumenter.filter { it.type == Dokument.Type.Inntektsmelding }
        if (inntektsmeldinger.size < 2) {
            return inntektsmeldinger.firstOrNull()?.dokumentId
        }

        logger.warn(
            "Fant ${inntektsmeldinger.size} inntektsmeldinger for vedtak med vedtaksperiodeId $vedtaksperiodeId, " +
                "bruker den nyeste (basert på innsendtTid) i dokumentkoblingen.",
        )
        return inntektsmeldinger
            .mapNotNull { inntektsmeldingRepository.hentMedInnsendingId(it.dokumentId) }
            .maxByOrNull { it.innsendtTid }
            ?.id
    }
}
