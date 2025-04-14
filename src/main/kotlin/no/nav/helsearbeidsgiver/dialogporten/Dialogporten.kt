package no.nav.helsearbeidsgiver.dialogporten

import kotlinx.coroutines.runBlocking
import no.nav.helsearbeidsgiver.Env
import no.nav.helsearbeidsgiver.auth.AltinnAuthClient
import no.nav.helsearbeidsgiver.auth.getDialogportenToken
import no.nav.helsearbeidsgiver.sykmelding.SendSykmeldingAivenKafkaMessage
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import java.util.UUID

interface IDialogportenService {
    fun opprettDialog(
        orgnr: String,
        forespoerselId: UUID,
    ): Result<String>

    fun opprettNyDialogMedSykmelding(
        orgnr: String,
        sykmeldingId: UUID,
        sykmeldingMessage: SendSykmeldingAivenKafkaMessage,
    ): Result<String>
}

class IngenDialogportenService : IDialogportenService {
    override fun opprettDialog(
        orgnr: String,
        forespoerselId: UUID,
    ): Result<String> {
        val generertId = UUID.randomUUID()
        sikkerLogger().info(
            "Oppretter ikke dialog for forespoerselId: {}, på orgnr: {}, generertId: {}",
            forespoerselId,
            orgnr,
            generertId,
        )
        return Result.success(generertId.toString())
    }

    override fun opprettNyDialogMedSykmelding(
        orgnr: String,
        forespoerselId: UUID,
        sykmeldingMessage: SendSykmeldingAivenKafkaMessage,
    ): Result<String> {
        val generertId = UUID.randomUUID()
        sikkerLogger().info(
            "Oppretter ikke dialog med sykmelding for : {}, på orgnr: {}, generertId: {}",
            forespoerselId,
            orgnr,
            generertId,
        )
        return Result.success(generertId.toString())
    }
}

class DialogportenService(
    val dialogportenClient: DialogportenClient,
) : IDialogportenService {
    private val navPortalBaseUrl = Env.getProperty("NAV_ARBEIDSGIVER_PORTAL_BASEURL")
    private val navApiBaseUrl = Env.getProperty("NAV_ARBEIDSGIVER_API_BASEURL")

    override fun opprettDialog(
        orgnr: String,
        forespoerselId: UUID,
    ): Result<String> =
        runBlocking {
            dialogportenClient
                .opprettDialog(
                    orgnr = orgnr,
                    url = "$navPortalBaseUrl/im-dialog/$forespoerselId",
                ).onFailure { e -> sikkerLogger().error("Fikk feil mot dialogporten", e) }
                .onSuccess { dialogId ->
                    sikkerLogger().info(
                        "Opprettet dialog for forespoerselId: {}, på orgnr: {}, med dialogId: {}",
                        forespoerselId,
                        orgnr,
                        dialogId,
                    )
                }
        }

    override fun opprettNyDialogMedSykmelding(
        orgnr: String,
        sykmeldingId: UUID,
        sykmeldingMessage: SendSykmeldingAivenKafkaMessage,
    ): Result<String> =
        runBlocking {
            dialogportenClient
                .opprettNyDialogMedSykmelding(
                    orgnr = orgnr,
                    dialogTittel = "Sykepenger for X Y (f. Z)",
                    dialogSammendrag = sykmeldingMessage.getSykmeldingsPerioderString(),
                    sykmeldingId = sykmeldingId,
                    sykmeldingJsonUrl = "$navApiBaseUrl/sykmelding/$sykmeldingId",
                ).onFailure { e -> sikkerLogger().error("Fikk feil mot dialogporten", e) }
                .onSuccess { dialogId ->
                    sikkerLogger().info(
                        "Opprettet dialog for sykmeldingId: {}, på orgnr: {}, med dialogId: {}",
                        sykmeldingId,
                        orgnr,
                        dialogId,
                    )
                }
        }
}

private fun SendSykmeldingAivenKafkaMessage.getSykmeldingsPerioderString(): String =
    when (this.sykmelding.sykmeldingsperioder.size) {
        0 -> "Ingen sykmeldingsperioder" // TODO: Håndtere dette tilfellet. Kaste feil?
        1 -> "Sykmeldingsperiode ${this.sykmelding.sykmeldingsperioder[0].fom} - ${this.sykmelding.sykmeldingsperioder[0].tom}"
        else ->
            "Sykmeldingsperioder ${this.sykmelding.sykmeldingsperioder.first().fom} - (...) - " +
                "${this.sykmelding.sykmeldingsperioder.last().tom}"
    }

fun lagDialogportenClient(authClient: AltinnAuthClient) =
    DialogportenClient(
        baseUrl = Env.getProperty("ALTINN_3_BASE_URL"),
        ressurs = Env.getProperty("ALTINN_IM_RESSURS"),
        getToken = authClient::getDialogportenToken,
    )
