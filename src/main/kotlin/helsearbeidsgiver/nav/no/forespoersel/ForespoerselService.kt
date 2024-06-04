
package helsearbeidsgiver.nav.no.forespoersel

import kotlinx.serialization.Serializable
import java.util.UUID

class ForespoerselService {
    fun hentForespoersler() : List<Forespoersel> {
        return listOf(Forespoersel(UUID.randomUUID().toString(), "ny"), Forespoersel(UUID.randomUUID().toString(), "besvart"))
    }
}
@Serializable
data class Forespoersel(val forespoerselId: String, val status: String)
