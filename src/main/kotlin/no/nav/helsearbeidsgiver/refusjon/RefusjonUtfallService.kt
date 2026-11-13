package no.nav.helsearbeidsgiver.refusjon

import java.util.UUID

class RefusjonUtfallService(
    private val refusjonKlient: RefusjonKlient,
    private val refusjonUtfallRepository: RefusjonUtfallRepository,
) {
    // "Trygg" metode som ikke parser json-payloads (gamle payloads kan mangle felter og serialisering vil feile):
    fun hentRefusjonUtfallMetadata(refusjonUtfallId: UUID): RefusjonUtfallMetadata? =
        refusjonUtfallRepository.hentRefusjonUtfallMetadata(refusjonUtfallId)

    suspend fun hentRefusjonUtfallPdf(refusjonUtfallId: UUID): ByteArray? = refusjonKlient.hentRefusjonUtfallPdf(refusjonUtfallId)

    // Vil feile på gamle payloads!

    // fun hentRefusjonUtfall(refusjonUtfallId: UUID): RefusjonUtfallResponse? = refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfallId)

    // fun hentRefusjonUtfall(filter: RefusjonUtfallFilter): List<RefusjonUtfallResponse> = refusjonUtfallRepository.hentRefusjonUtfall(filter)
}
