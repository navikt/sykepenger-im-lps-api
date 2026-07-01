package no.nav.helsearbeidsgiver.inntektsmelding

import no.nav.helsearbeidsgiver.dokumentkobling.DokumentkoblingService
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.innsending.Valideringsfeil
import no.nav.helsearbeidsgiver.utils.log.logger

class AvvistInntektsmeldingService(
    private val inntektsmeldingRepository: InntektsmeldingRepository,
    private val dokumentkoblingService: DokumentkoblingService,
) {
    private val logger = logger()

    fun oppdaterInnteksmeldingTilFeilet(avvistInntektsmelding: AvvistInntektsmelding) {
        if (inntektsmeldingRepository.hentMedInnsendingId(avvistInntektsmelding.inntektsmeldingId)?.status != InnsendingStatus.MOTTATT) {
            logger.warn(
                "Mottok melding om avvist inntektsmelding med id ${avvistInntektsmelding.inntektsmeldingId}, men er allerede behandlet, ignorerer",
            )
            return
        }
        when (
            inntektsmeldingRepository.oppdaterFeilstatusOgFeilkode(avvistInntektsmelding)
        ) {
            0 -> {
                logger.error(
                    "Klarte ikke å oppdatere inntektsmelding ${avvistInntektsmelding.inntektsmeldingId} til status FEILET " +
                        "med feilkode ${avvistInntektsmelding.feil.feilkode} fordi den ikke finnes i databasen.",
                )
            }

            else -> {
                logger.info(
                    "Oppdaterte inntektsmelding ${avvistInntektsmelding.inntektsmeldingId} til status FEILET " +
                        "med feilkode ${avvistInntektsmelding.feil.feilkode}.",
                )
                dokumentkoblingService.produserInntektsmeldingAvvistKobling(
                    avvistInntektsmelding,
                )
            }
        }
    }
}
