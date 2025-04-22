package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.auth.dialogportenTokenGetter
import no.nav.helsearbeidsgiver.felles.auth.AuthClient
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import no.nav.helsearbeidsgiver.utils.wrapper.Fnr
import java.util.UUID

interface IDialogportenService {
    fun opprettNyDialogMedSykmelding(
        orgnr: String,
        sykmeldingId: UUID,
        sykmeldingMessage: SendSykmeldingAivenKafkaMessage,
    ): String
}

class IngenDialogportenService : IDialogportenService {
    override fun opprettNyDialogMedSykmelding(
        orgnr: String,
        forespoerselId: UUID,
        sykmeldingMessage: SendSykmeldingAivenKafkaMessage,
    ): String {
        val generertId = UUID.randomUUID()
        sikkerLogger().info("Oppretter ikke dialog med sykmelding for : $forespoerselId, på orgnr: $orgnr, generertId: $generertId")
        return generertId.toString()
    }
}

class DialogportenService(
    val dialogportenClient: DialogportenClient,
) : IDialogportenService {
    private val navPortalBaseUrl = Env.getProperty("NAV_ARBEIDSGIVER_PORTAL_BASEURL")
    private val navApiBaseUrl = Env.getProperty("NAV_ARBEIDSGIVER_API_BASEURL")

    override fun opprettNyDialogMedSykmelding(
        orgnr: String,
        sykmeldingId: UUID,
        sykmeldingMessage: SendSykmeldingAivenKafkaMessage,
    ): String =
        runBlocking {
            dialogportenClient
                .opprettNyDialogMedSykmelding(
                    orgnr = orgnr,
                    dialogTittel = "Sykepenger for Fornavn Etternavnsen (f. ${sykmeldingMessage.getFoedselsdatoString()})",
                    dialogSammendrag = sykmeldingMessage.getSykmeldingsPerioderString(),
                    sykmeldingId = sykmeldingId,
                    sykmeldingJsonUrl = "$navApiBaseUrl/sykmelding/$sykmeldingId",
                )
        }
}

private fun SendSykmeldingAivenKafkaMessage.getSykmeldingsPerioderString(): String =
    when (this.sykmelding.sykmeldingsperioder.size) {
        1 -> "Sykmeldingsperiode ${this.sykmelding.sykmeldingsperioder[0].fom} - ${this.sykmelding.sykmeldingsperioder[0].tom}"
        else ->
            "Sykmeldingsperioder ${this.sykmelding.sykmeldingsperioder.first().fom} - (...) - " +
                "${this.sykmelding.sykmeldingsperioder.last().tom}"
    }

// Støtter d-nummer
private fun SendSykmeldingAivenKafkaMessage.getFoedselsdatoString(): String {
    val fnr = Fnr(this.kafkaMetadata.fnr)
    val foersteSiffer = fnr.verdi.first().digitToInt()
    return if (foersteSiffer < 4) {
        fnr.verdi.take(6)
    } else {
        (foersteSiffer - 4).toString() + fnr.verdi.substring(1, 6)
    }
}

fun lagDialogportenClient(authClient: AuthClient) =
    DialogportenClient(
        baseUrl = Env.getProperty("ALTINN_3_BASE_URL"),
        ressurs = Env.getProperty("ALTINN_IM_RESSURS"),
        getToken = authClient.dialogportenTokenGetter(),
    )
