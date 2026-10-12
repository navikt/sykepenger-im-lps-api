package no.nav.helsearbeidsgiver.dokumentkobling

import no.nav.helsearbeidsgiver.config.Repositories
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Periode
import no.nav.helsearbeidsgiver.forespoersel.Forespoersel
import no.nav.helsearbeidsgiver.inntektsmelding.AvvistInntektsmelding
import no.nav.helsearbeidsgiver.inntektsmelding.InnsendingType
import no.nav.helsearbeidsgiver.kafka.forespoersel.pri.ForespoerselDokument
import no.nav.helsearbeidsgiver.kafka.sis.Dokument
import no.nav.helsearbeidsgiver.pdl.domene.FullPerson
import no.nav.helsearbeidsgiver.refusjon.RefusjonUtfall
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.utils.log.logger
import no.nav.helsearbeidsgiver.utils.wrapper.Orgnr
import java.util.UUID

class DokumentkoblingService(
    val dokumentkoblingProducer: DokumentkoblingProducer,
    val repositories: Repositories,
) {
    private val logger = logger()

    fun produserSykmeldingKobling(
        sykmeldingId: UUID,
        sykmeldingMessage: SendSykmeldingAivenKafkaMessage,
        fullPerson: FullPerson,
    ) {
        val orgnr = Orgnr(sykmeldingMessage.event.arbeidsgiver.orgnummer)

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
    }

    fun produserSykepengesoeknadKobling(
        soeknadId: UUID,
        sykmeldingId: UUID,
        orgnr: Orgnr,
        korrigerer: UUID? = null,
    ) {
        val sykepengesoeknadKobling =
            Sykepengesoeknad(
                soeknadId = soeknadId,
                sykmeldingId = sykmeldingId,
                orgnr = orgnr,
                korrigerer = korrigerer,
            )
        dokumentkoblingProducer.send(sykepengesoeknadKobling)
        logger.info(
            "Sendte melding på helsearbeidsgiver.dokument-kobling for sykepengesøknad med " +
                "soeknadId: $soeknadId og sykmeldingId: $sykmeldingId",
        )
    }

    fun produserVedtaksperiodeSoeknadKobling(
        vedtaksperiodeId: UUID,
        soeknadId: UUID,
    ) {
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

    fun produserForespoerselKobling(forespoersel: ForespoerselDokument) {
        val orgnr = Orgnr(forespoersel.orgnr)
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
    }

    fun oppdaterDialogMedUtgaattForespoersel(forespoersel: Forespoersel) {
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

        dokumentkoblingProducer.send(inntektsmeldingGodkjent)

        logger.info(
            "Sendte melding til hag-dialog på helsearbeidsgiver.dokument-kobling for inntektsmelding Godkjent med innsendingsId: ${inntektsmeldingGodkjent.inntektsmeldingId}, vedtaksperiodeId: ${inntektsmeldingGodkjent.vedtaksperiodeId}.",
        )
    }

    fun produserVedtakKobling(refusjonUtfall: RefusjonUtfall) {
        val melding = refusjonUtfall.arbeidstakerVedtakMelding
        val vedtaksperiodeId = refusjonUtfall.vedtaksperiodeId

        if (!melding.harArbeidsgiverOensketRefusjon) {
            logger.info(
                "Sender _ikke_ melding på helsearbeidsgiver.dokument-kobling for refusjonsutfall med vedtaksperiodeId " +
                    "$vedtaksperiodeId, fordi arbeidsgiver ikke har ønsket refusjon.",
            )
            return
        }

        val sykmeldingId = melding.dokumenter.firstOrNull { it.type == Dokument.Type.Sykmelding }?.dokumentId
        val inntektsmeldingId =
            melding.dokumenter
                .filter { it.type == Dokument.Type.Inntektsmelding }
                .mapNotNull { repositories.inntektsmeldingRepository.hentMedInnsendingId(it.dokumentId) }
                .maxByOrNull { it.innsendtTid }
                ?.id

        if (sykmeldingId == null || inntektsmeldingId == null) {
            logger.warn(
                "Mangler sykmeldingId og/eller inntektsmeldingId for refusjonsutfall med vedtaksperiodeId " +
                    "$vedtaksperiodeId (sykmeldingId=$sykmeldingId, inntektsmeldingId=$inntektsmeldingId), " +
                    "sender ikke melding på helsearbeidsgiver.dokument-kobling.",
            )
            return
        }

        dokumentkoblingProducer.send(
            Vedtak(
                vedtakId = refusjonUtfall.refusjonUtfallId,
                sykmeldingId = sykmeldingId,
                inntektsmeldingId = inntektsmeldingId,
                orgnr = refusjonUtfall.orgnr,
            ),
        )
        logger.info(
            "Sendte melding på helsearbeidsgiver.dokument-kobling for refusjonsutfall med " +
                "refusjonUtfallId: ${refusjonUtfall.refusjonUtfallId} og vedtaksperiodeId: $vedtaksperiodeId",
        )
    }

    fun produserInntektsmeldingAvvistKobling(avvistInntektsmelding: AvvistInntektsmelding) {
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
    }
}
