package no.nav.helsearbeidsgiver.dokumentkobling

import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.inntektsmelding.AvvistInntektsmelding
import no.nav.helsearbeidsgiver.inntektsmelding.InnsendingType
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
    val repositories: Repositories,
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

    fun produserForespoerselKobling(forespoersel: ForespoerselDokument) {
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
                "Sendte melding på helsearbeidsgiver.dokument-kobling for inntektsmeldingsforespørsel med id: ${forespoersel.forespoerselId}, vedtaksperiodeId: ${forespoersel.vedtaksperiodeId}.",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding på helsearbeidsgiver.dokument-kobling for inntektsmeldingsforespørsel med id: ${forespoersel.forespoerselId}, fordi feature toggle er av.",
            )
        }
    }

    fun oppdaterDialogMedUtgaattForespoersel(forespoersel: Forespoersel) {
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmeldingsforespoersel(orgnr = Orgnr(forespoersel.orgnr))) {
            val vedtaksperiodeId =
                repositories.forespoerselRepository.hentVedtaksperiodeId(forespoersel.navReferanseId)
                    ?: run {
                        // TODO: kan vi finne en bedre måte å håndtere dette på?
                        logger.warn(
                            "Fant ingen vedtaksperiodeId for utgått inntektsmeldingsforespørsel med id: ${forespoersel.navReferanseId}. " +
                                "Kan derfor ikke produsere dialogmelding på helsearbeidsgiver.dokument-kobling.",
                        )
                        return
                    }
            dokumentkoblingProducer.send(
                ForespoerselUtgaatt(
                    forespoerselId = forespoersel.navReferanseId,
                    vedtaksperiodeId = vedtaksperiodeId,
                    orgnr = Orgnr(forespoersel.orgnr),
                ),
            )

            logger.info(
                "Sendte melding på helsearbeidsgiver.dokument-kobling for utgått inntektsmeldingsforespørsel med id: ${forespoersel.navReferanseId}, vedtaksperiodeId: $vedtaksperiodeId.",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding på helsearbeidsgiver.dokument-kobling for utgått inntektsmeldingsforespørsel med id: ${forespoersel.navReferanseId}, fordi feature toggle er av.",
            )
        }
    }

    private fun Inntektsmelding.Type.manglerForespoersel(): Boolean =
        when (this) {
            is Inntektsmelding.Type.Forespurt,
            is Inntektsmelding.Type.ForespurtEkstern,
            -> false
            else -> true
        }

    fun produserInntektsmeldingGodkjentKobling(inntektsmelding: Inntektsmelding) {
        val vedtaksperiodeId = inntektsmelding.vedtaksperiodeId
        if (vedtaksperiodeId == null) {
            logger.warn(
                "Klarte ikke å finne alle data til dokumentkobling for inntektsmelding med id: ${inntektsmelding.id} med type: ${inntektsmelding.type::class.simpleName} sender ikke melding på helsearbeidsgiver.dokument-kobling .",
            )
            return
        }
        if (inntektsmelding.type.manglerForespoersel()) {
            logger.warn(
                "Inntektsmelding med id: ${inntektsmelding.id} er ikke av forespurt type, sender ikke melding på helsearbeidsgiver.dokument-kobling .",
            )
            return
        }
        val inntektsmeldingGodkjent =
            InntektsmeldingGodkjent(
                inntektsmeldingId = inntektsmelding.id,
                forespoerselId = inntektsmelding.type.id,
                vedtaksperiodeId = vedtaksperiodeId,
                orgnr = inntektsmelding.avsender.orgnr,
                innsendingType = InnsendingType.from(inntektsmelding.type),
            )
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmelding(inntektsmeldingGodkjent.orgnr.verdi)) {
            dokumentkoblingProducer.send(
                inntektsmeldingGodkjent,
            )
            logger.info(
                "Sendte melding til hag-dialog på helsearbeidsgiver.dokument-kobling for inntektsmelding Godkjent med innsendingsId: ${inntektsmeldingGodkjent.inntektsmeldingId}, vedtaksperiodeId: ${inntektsmeldingGodkjent.vedtaksperiodeId}.",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding på helsearbeidsgiver.dokument-kobling for inntektsmelding Godkjent med innsendingsId: ${inntektsmeldingGodkjent.inntektsmeldingId}, vedtaksperiodeId: ${inntektsmeldingGodkjent.vedtaksperiodeId}",
            )
        }
    }

    fun produserInntektsmeldingAvvistKobling(avvistInntektsmelding: AvvistInntektsmelding) {
        if (unleashFeatureToggles.skalOppdatereDialogVedMottattInntektsmelding(avvistInntektsmelding.orgnr.verdi)) {
            val dokumentkoblingImAvvist =
                InntektsmeldingAvvist(
                    inntektsmeldingId = avvistInntektsmelding.inntektsmeldingId,
                    forespoerselId = avvistInntektsmelding.forespoerselId,
                    vedtaksperiodeId = avvistInntektsmelding.vedtaksperiodeId,
                    orgnr = avvistInntektsmelding.orgnr,
                )
            dokumentkoblingProducer.send(
                dokumentkoblingImAvvist,
            )
            logger.info(
                "Sendte melding til hag-dialog på helsearbeidsgiver.dokument-kobling for inntektsmelding Avvist med innsendingsId: ${dokumentkoblingImAvvist.inntektsmeldingId}, vedtaksperiodeId: ${dokumentkoblingImAvvist.vedtaksperiodeId}.",
            )
        } else {
            logger.info(
                "Sendte _ikke_ melding på helsearbeidsgiver.dokument-kobling for inntektsmelding Avvist med innsendingsId: ${avvistInntektsmelding.inntektsmeldingId}, vedtaksperiodeId: ${avvistInntektsmelding.vedtaksperiodeId}",
            )
        }
    }
}
