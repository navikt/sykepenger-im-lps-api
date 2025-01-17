package no.nav.helsearbeidsgiver.kafka.inntektsmelding

import no.nav.helsearbeidsgiver.domene.inntektsmelding.v1.JournalfoertInntektsmelding
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
                sikkerLogger.info("Ugyldig event, ignorerer melding")
                mottakRepository.opprett(ExposedMottak(melding = melding, gyldig = false))
                return
            }
        transaction {
            try {
                inntektsmeldingService.opprettInntektsmelding(obj.inntektsmelding)
                mottakRepository.opprett(ExposedMottak(melding))
            } catch (e: Exception) {
                rollback()
                sikkerLogger.error("Klarte ikke å lagre i database!", e)
                throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
            }
        }
    }

    private fun parseRecord(record: String): JournalfoertInntektsmelding = jsonMapper.decodeFromString<JournalfoertInntektsmelding>(record)
}
