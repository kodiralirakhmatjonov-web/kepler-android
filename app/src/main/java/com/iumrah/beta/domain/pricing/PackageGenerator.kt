package com.iumrah.beta.domain.pricing

import com.iumrah.beta.data.hotel.RemotePackageEngineClient
import com.iumrah.beta.domain.journey.JourneyState

/** Android now uses the same server-authoritative PackageEngine as iOS. */
class PackageGenerator(private val packageEngine: RemotePackageEngineClient) {
    suspend fun generate(state: JourneyState): PackageQuote = packageEngine.packageQuote(state)
}
