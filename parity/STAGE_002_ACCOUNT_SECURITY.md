# Stage 002 — Account / API / Security parity

Ported from the current SwiftUI source-of-truth:

- `Sources/Networking/APIClient.swift`
- `Sources/Core/IumrahAccountSecurityIdentity.swift`
- `Sources/Core/IumrahClientIdentity.swift` (legacy identity remains intentionally removed)
- `Sources/Models/IumrahAccountModels.swift`
- `Sources/Models/IumrahSecurityConfirmationModels.swift`
- `Sources/Models/IumrahFriendsModels.swift`
- account/trip decode subset of `Sources/Models/BookingModels.swift`
- `Sources/Services/IumrahAccountService.swift`
- `Sources/State/IumrahAccountStore.swift`

Security parity:

- persistent installation ID + 256-bit random device secret
- Android Keystore-backed AES-256-GCM storage; no plaintext account token storage
- same bearer token, `x-booking-token`, `x-iumrah-device-id`, and `x-iumrah-device-secret` contracts
- same account/session/profile/trips/checkout/traveler/passport/receipt/security/email/recovery endpoints
- Android reports `platform=Android`; the server remains the single source of session/location truth

Apple sign-in transport models and server routes are preserved. Android browser/OAuth UX is intentionally deferred to the Account UI stage; it is not silently deleted.
