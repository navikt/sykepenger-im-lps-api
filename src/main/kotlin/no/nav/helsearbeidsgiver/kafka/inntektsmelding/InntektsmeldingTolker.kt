package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.Inntektsmelding
import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.JournalfoertInntektsmelding
import no.nav.helsearbeidsgiver.innsending.InnsendingStatus
import no.nav.helsearbeidsgiver.inntektsmelding.InntektsmeldingService
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.mottak.ExposedMottak
import no.nav.helsearbeidsgiver.mottak.MottakRepository
import no.nav.helsearbeidsgiver.utils.jsonMapper
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

class InntektsmeldingTolker(
    private val inntektsmeldingService: InntektsmeldingService,
    private val mottakRepository: MottakRepository,
) : MeldingTolker {
    private val sikkerLogger = LoggerFactory.getLogger("tjenestekall")

    override fun lesMelding(melding: String) {
        sikkerLogger.info("Mottatt IM: $melding")
        val obj =
            try {
                parseRecord(melding)
            } catch (e: Exception) {
                sikkerLogger.error("Ugyldig inntektsmeldingformat!", e)
                throw e // ikke gå videre ved feil
            }
        transaction {
            try {
                if (innsendtFraNavPortal(obj.inntektsmelding.type)) { // TODO: flytt denne sjekken inn i Service
                    sikkerLogger.info("Mottok im sendt fra NAV PORTAL - lagrer")
                    inntektsmeldingService.opprettInntektsmelding(obj.inntektsmelding)
                } else {
                    sikkerLogger.info("Mottok im sendt fra LPS - oppdaterer status")
                    inntektsmeldingService.oppdaterStatus(obj.inntektsmelding, InnsendingStatus.GODKJENT)
                }
                mottakRepository.opprett(ExposedMottak(melding))
            } catch (e: Exception) {
                rollback()
                sikkerLogger.error("Klarte ikke å lagre i database!", e)
                throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
            }
        }
    }

    private fun innsendtFraNavPortal(type: Inntektsmelding.Type): Boolean =
        type is Inntektsmelding.Type.Forespurt || type is Inntektsmelding.Type.Selvbestemt

    private fun parseRecord(record: String): JournalfoertInntektsmelding = jsonMapper.decodeFromString<JournalfoertInntektsmelding>(record)
}
