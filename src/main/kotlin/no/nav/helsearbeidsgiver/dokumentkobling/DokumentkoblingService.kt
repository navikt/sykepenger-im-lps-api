package no.nav.helsearbeidsgiver.dokumentkobling

import no.nav.helsearbeidsgiver.dialogporten.DialogInntektsmeldingsforespoersel
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.ForespoerselDokument
import no.nav.helsearbeidsgiver.pdl.domene.FullPerson
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class DokumentkoblingService(
    val dokumentkoblingProducer: DokumentkoblingProducer,
    val unleashFeatureToggles: UnleashFeatureToggles,
) {
    private val logger = logger()

    fun produserSykmeldingKobling(
        sykmeldingId: UUID,
        sykmeldingMessage: SendSykmeldingAivenKafkaMessage,
        fullPerson: FullPerson,
    ) {
        val orgnr = Orgnr(sykmeldingMessage.event.arbeidsgiver.orgnummer)

        if (unleashFeatureToggles.skalOppretteDialogVedMottattSykmelding(orgnr)) {
            val sykmeldingKobling =
                Sykmelding(
                    sykmeldingId = sykmeldingId,
                    orgnr = orgnr,
                    foedselsdato = fullPerson.foedselsdato,
                    fulltNavn = fullPerson.navn.fulltNavn(),
                    sykmeldingsperioder =
                        sykmeldingMessage.sykmelding.sykmeldingsperioder.map {
                            Periode(
                                it.fom,
                                it.tom,
                            )
                        },
                )
            dokumentkoblingProducer.send(sykmeldingKobling)
            logger.info(
                "Sendte melding på helsearbeidsgiver.dokument-kobling for sykmelding med sykmeldingId: $sykmeldingId",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding på helsearbeidsgiver.dokument-kobling med " +
                    "sykmeldingId: $sykmeldingId fordi feature toggle er av.",
            )
        }
    }

    fun produserSykepengesoeknadKobling(
        soeknadId: UUID,
        sykmeldingId: UUID,
        orgnr: Orgnr,
    ) {
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattSoeknad(orgnr)) {
            val sykepengesoeknadKobling =
                Sykepengesoeknad(
                    soeknadId = soeknadId,
                    sykmeldingId = sykmeldingId,
                    orgnr = orgnr,
                )
            dokumentkoblingProducer.send(sykepengesoeknadKobling)
            logger.info(
                "Sendte melding på helsearbeidsgiver.dokument-kobling for sykepengesøknad med " +
                    "soeknadId: $soeknadId og sykmeldingId: $sykmeldingId",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding på helsearbeidsgiver.dokument-kobling for sykepengesøknad med " +
                    "søknadId: $soeknadId, sykmeldingId: $sykmeldingId, fordi feature toggle er av.",
            )
        }
    }

    fun produserVedtaksperiodeSoeknadKobling(
        vedtaksperiodeId: UUID,
        soeknadId: UUID,
    ) {
        if (unleashFeatureToggles.skalSendeVedtaksperiodeSoeknadKoblinger()) {
            val vedtaksperiodeSoeknadKobling =
                VedtaksperiodeSoeknadKobling(
                    vedtaksperiodeId = vedtaksperiodeId,
                    soeknadId = soeknadId,
                )
            dokumentkoblingProducer.send(vedtaksperiodeSoeknadKobling)
            logger.info(
                "Sendte melding på helsearbeidsgiver.dokument-kobling for vedtaksperiode-søknad-kobling med " +
                    "vedtaksperiodeId: $vedtaksperiodeId og soeknadId: $soeknadId",
            )
        }
    }

    fun oppdaterDialogMedInntektsmeldingsforespoersel(forespoersel: ForespoerselDokument) {
        val orgnr = Orgnr(forespoersel.orgnr)
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr = orgnr)) {
            dokumentkoblingProducer.send(
                ForespoerselSendt(
                    forespoerselId = forespoersel.forespoerselId,
                    vedtaksperiodeId = forespoersel.vedtaksperiodeId,
                    orgnr = orgnr,
                ),
            )

            logger.info(
                "Sendte melding til hag-dialog for inntektsmeldingsforespørsel med id: ${forespoersel.forespoerselId}, vedtaksperiodeId: ${forespoersel.vedtaksperiodeId}.",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding til hag-dialog for inntektsmeldingsforespørsel med id: ${forespoersel.forespoerselId}, fordi feature toggle er av.",
            )
        }
    }
}
