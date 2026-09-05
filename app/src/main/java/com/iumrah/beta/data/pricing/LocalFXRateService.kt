package com.iumrah.beta.data.pricing

import java.math.BigDecimal
import java.math.MathContext
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/** Android parity port of the iOS CBU-backed FX normalizer. */
class LocalFXRateService(
    private val client: OkHttpClient = OkHttpClient(),
) {
    @Serializable
    private data class CBUEntry(@SerialName("Ccy") val ccy: String, @SerialName("Rate") val rate: String)

    private val json = Json { ignoreUnknownKeys = true }
    private var cached: Map<String, BigDecimal> = mapOf("USD" to BigDecimal.ONE)
    private var cachedAt: Instant? = null

    suspend fun usd(amount: BigDecimal, rawCurrency: String): BigDecimal {
        require(amount >= BigDecimal.ZERO) { "Некорректная сумма компонента." }
        val currency = rawCurrency.uppercase()
        if (currency == "USD") return amount
        refreshIfNeeded()
        val sourceUZS = cached[currency] ?: throw IllegalStateException("Не удалось пересчитать валюту $currency в USD.")
        val usdUZS = cached["USD"]?.takeIf { it > BigDecimal.ZERO }
            ?: throw IllegalStateException("Курс валют временно недоступен. Повторите расчёт.")
        return amount.multiply(sourceUZS, MathContext.DECIMAL128).divide(usdUZS, MathContext.DECIMAL128)
    }

    private suspend fun refreshIfNeeded() {
        val now = Instant.now()
        val fresh = cachedAt?.let { ChronoUnit.HOURS.between(it, now) < 6 } == true && cached.size > 1
        if (fresh) return
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://cbu.uz/en/arkhiv-kursov-valyut/json/")
                .header("Accept", "application/json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IllegalStateException("Курс валют временно недоступен. Повторите расчёт.")
                val payload = response.body.string()
                val rows = json.decodeFromString(ListSerializer(CBUEntry.serializer()), payload)
                val next = rows.mapNotNull { row ->
                    val value = row.rate.replace(" ", "").replace(',', '.').toBigDecimalOrNull()
                    if (value != null && value > BigDecimal.ZERO) row.ccy.uppercase() to value else null
                }.toMap()
                if (next["USD"] == null) throw IllegalStateException("Курс валют временно недоступен. Повторите расчёт.")
                cached = next
                cachedAt = now
            }
        }
    }
}
