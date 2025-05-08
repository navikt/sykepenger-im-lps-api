package no.nav.helsearbeidsgiver.soknad

import no.nav.helsearbeidsgiver.kafka.soknad.SykepengesoknadDTO

class SoknadService(
    val soknadRepository: SoknadRepository,
) {
    fun lagreSoknad(soknad: SykepengesoknadDTO) {
        soknadRepository.lagreSoknad(soknad)
    }
}
