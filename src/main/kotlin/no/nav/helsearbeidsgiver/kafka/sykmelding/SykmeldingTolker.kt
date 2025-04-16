package no.nav.helsearbeidsgiver.kafka.sykmelding

// import no.nav.helsearbeidsgiver.pdl.Behandlingsgrunnlag

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.kafka.MeldingTolker
import no.nav.helsearbeidsgiver.pdl.Behandlingsgrunnlag
import no.nav.helsearbeidsgiver.pdl.PdlClient
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.sykmelding.SykmeldingService
import no.nav.helsearbeidsgiver.tokenprovider.OAuth2Environment
import no.nav.helsearbeidsgiver.tokenprovider.oauth2ClientCredentialsTokenGetter
import no.nav.helsearbeidsgiver.utils.UnleashFeatureToggles
import no.nav.helsearbeidsgiver.utils.jsonMapper
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger

// val pdlClient = opprettSykmeldingPdlClient()
// val pdlClient = PdlClient(url = "", getAccessToken = () -> "", Behandlingsgrunnlag.SYKMELDING)

class SykmeldingTolker(
    private val sykmeldingService: SykmeldingService,
    private val unleashFeatureToggles: UnleashFeatureToggles,
) : MeldingTolker {
    private val sikkerLogger = sikkerLogger()

    override fun lesMelding(melding: String) {
        try {
            val sykmeldingMessage = jsonMapper.decodeFromString<SendSykmeldingAivenKafkaMessage>(melding)

            runBlocking {
//                val fnr = sykmeldingMessage.kafkaMetadata.fnr
//                val person = pdlClient.personBolk(listOf(fnr))?.firstOrNull()
//                person ?: throw RuntimeException("Fant ikke person i PDL oppslag [fnr:$fnr]")
//                val sykmeldtNavn = person.navn.fulltNavn()
                val sykmeldtNavn = "abc"

                sykmeldingService.lagreSykmelding(sykmeldingMessage, sykmeldtNavn)
                sikkerLogger.error("Lagret sykmelding til database med id: ${sykmeldingMessage.sykmelding.id}")

                if (unleashFeatureToggles.skalOppretteDialogVedMottattSykmelding()) {
                    sikkerLogger()
                        .info("Unleash toggle for å opprette Dialogporten dialog er skrudd på (dialogopprettelse ikke implementert ennå).")
                } else {
                    sikkerLogger()
                        .info("Unleash toggle for å opprette Dialogporten dialog er skrudd av.")
                }
            }
        } catch (e: Exception) {
            sikkerLogger.error("Klarte ikke å lagre sykmelding i database!", e)
            throw e // sørg for at kafka-offset ikke commites dersom vi ikke lagrer i db
        }
    }
}

private fun opprettSykmeldingPdlClient(): PdlClient {
    val pdlUrl = Env.getProperty("PDL_URL")

    val oauth2Environment =
        OAuth2Environment(
            scope = Env.getProperty("PDL_SCOPE"),
            wellKnownUrl = Env.getProperty("AZURE_APP_WELL_KNOWN_URL"),
            tokenEndpointUrl = Env.getProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT"),
            clientId = Env.getProperty("AZURE_APP_CLIENT_ID"),
            clientSecret = Env.getProperty("AZURE_APP_CLIENT_SECRET"),
            clientJwk = Env.getProperty("AZURE_APP_JWK"),
        )
    val tokenGetter = oauth2ClientCredentialsTokenGetter(oauth2Environment)
    return PdlClient(url = pdlUrl, getAccessToken = tokenGetter, behandlingsgrunnlag = Behandlingsgrunnlag.SYKMELDING)
}
