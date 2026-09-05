# Android Stage 006–008 unified parity

- Same API base: https://iumrah.app. No Android-specific business database.
- Stage 006: CBU FX normalization, room-category propagation, complete-journey flight fare charged once, Final Package. Android beta test package markup remains 20%; 2% payment fee and $5 public rounding remain unchanged.
- Stage 007: booking create/token vault, operational sync for iumrah Business, checkout, hotel replacement, contacts/customization, identity confirmation. Passport number is transient UI state and is not stored in the Android booking vault.
- Stage 008: iumrah Care messages/attachments/read state, Care profile, server notification feed and Android runtime notification permission.
- FCM credentials are intentionally not fabricated or committed. Device registration accepts a real token later; server feed works without one.
- Motion parity includes iumrah press springs and a native Compose Canvas booking Dome flip/spectral animation instead of Material defaults.
