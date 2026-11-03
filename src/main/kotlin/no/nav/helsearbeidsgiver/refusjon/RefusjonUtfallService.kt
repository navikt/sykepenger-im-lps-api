package no.nav.helsearbeidsgiver.refusjon

import java.util.UUID

class RefusjonUtfallService(
    private val refusjonKlient: RefusjonKlient,
    private val refusjonUtfallRepository: RefusjonUtfallRepository,
) {
    fun hentRefusjonUtfall(refusjonUtfallId: UUID): RefusjonUtfallResponse? = refusjonUtfallRepository.hentRefusjonUtfall(refusjonUtfallId)

    fun hentRefusjonUtfall(filter: RefusjonUtfallFilter): List<RefusjonUtfallResponse> = refusjonUtfallRepository.hentRefusjonUtfall(filter)

    suspend fun hentRefusjonUtfallPdf(refusjonUtfallId: UUID): ByteArray? = refusjonKlient.hentRefusjonUtfallPdf(refusjonUtfallId)
}
