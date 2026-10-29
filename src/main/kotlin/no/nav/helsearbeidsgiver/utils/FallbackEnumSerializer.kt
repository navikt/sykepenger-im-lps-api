package no.nav.helsearbeidsgiver.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import no.nav.helsearbeidsgiver.utils.log.sikkerLogger
import kotlin.enums.EnumEntries

/** Mapper ukjente enum-verdier til [fallback] i stedet for å feile hele meldingen. */
abstract class FallbackEnumSerializer<T : Enum<T>>(
    entries: EnumEntries<T>,
    private val standardSerializer: KSerializer<T>,
    private val fallback: T,
) : KSerializer<T> {
    private val valueByName: Map<String, T> =
        entries.associateBy { standardSerializer.descriptor.getElementName(it.ordinal) }

    override val descriptor: SerialDescriptor = standardSerializer.descriptor

    override fun serialize(
        encoder: Encoder,
        value: T,
    ) = standardSerializer.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): T {
        val name = decoder.decodeString()
        return valueByName[name] ?: fallback.also {
            sikkerLogger().warn("Mottok ukjent verdi '$name' for ${descriptor.serialName}, fallbacker til $fallback")
        }
    }
}
