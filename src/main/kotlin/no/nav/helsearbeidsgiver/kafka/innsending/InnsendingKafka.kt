package no.nav.helsearbeidsgiver.kafka.innsending

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.JsonElement
import no.nav.helsearbeidsgiver.utils.json.serializer.AsStringSerializer
import no.nav.helsearbeidsgiver.utils.json.toJson

object InnsendingKafka {
    @Serializable
    enum class EventName {
        API_INNSENDING_STARTET,
    }

    @Serializable(KeySerializer::class)
    enum class Key {
        EVENT_NAME,
        KONTEKST_ID,
        DATA,
        MOTTATT,
        INNSENDING,
        ;

        override fun toString(): String =
            when (this) {
                EVENT_NAME -> "@event_name"
                else -> name.lowercase()
            }

        companion object {
            internal fun fromString(key: String): Key =
                Key.entries.firstOrNull {
                    key == it.toString()
                }
                    ?: throw IllegalArgumentException("Fant ingen Key med verdi som matchet '$key'.")
        }
    }

    fun EventName.toJson(): JsonElement = toJson(EventName.serializer())

    internal object KeySerializer : AsStringSerializer<Key>(
        serialName = "helsearbeidsgiver.kotlinx.sykepenger-im-lps-api.innsending.Key",
        parse = Key.Companion::fromString,
    )

    fun Map<Key, JsonElement>.toJson(): JsonElement =
        toJson(
            MapSerializer(
                Key.serializer(),
                JsonElement.serializer(),
            ),
        )
}
