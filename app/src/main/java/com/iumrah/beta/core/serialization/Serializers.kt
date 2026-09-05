package com.iumrah.beta.core.serialization

import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

object BigDecimalJsonSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): BigDecimal = when (decoder) {
        is JsonDecoder -> decoder.decodeJsonElement().jsonPrimitive.content.toBigDecimal()
        else -> decoder.decodeString().toBigDecimal()
    }
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        if (encoder is JsonEncoder) encoder.encodeJsonElement(JsonPrimitive(value)) else encoder.encodeString(value.toPlainString())
    }
}

object InstantIsoSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InstantISO8601", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())
}


object LocalDateIsoSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateISO8601", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())
}
