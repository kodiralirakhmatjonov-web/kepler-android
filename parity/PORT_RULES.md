# iumrah Android Port Rules

1. The uploaded iOS repository is the source of truth for behavior and contracts.
2. Android is a native Kotlin/Jetpack Compose client, not a second business system.
3. `https://iumrah.app` remains the API base.
4. HOTELS_DB / BOOKINGS_DB remain server-side shared infrastructure used by iOS, Android and iumrah Business through backend APIs.
5. Never create an Android-only booking database to work around missing APIs.
6. Preserve wire names, enum raw values, component codes and booking payload semantics.
7. Preserve Expedia-style round-trip/open-jaw journey pricing.
8. Package markup fallback is 50%, matching the current iOS fallback. Production configurator quotes remain server-authoritative.
9. Any future intentional divergence must be recorded in this file and in a golden test.
10. A feature is not marked ported until both behavior and API contract are implemented.
