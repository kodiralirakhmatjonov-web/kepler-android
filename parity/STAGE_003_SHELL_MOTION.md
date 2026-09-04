# Stage 003 — App shell + native motion parity

This stage ports the SwiftUI shell, cinematic onboarding, Home foundation and motion language into native Jetpack Compose.

## Motion contract copied from the iOS source

- Primary/secondary controls: pressed scale `0.985`, source spring response ≈ `0.24`, damping `0.86`.
- Pressable cards: a slightly deeper `0.978` scale while held, with the same native spring family.
- Root onboarding ↔ app transition: opacity + scale (`0.985` / `1.015`) around 280–340 ms.
- Bottom-tab selection: native spring with haptic selection feedback.
- Onboarding page scene: interactive page scaling around `0.965`, native spring, 240 ms page snap.
- Home emotional captions: vapor-style reveal with 820 ms ease-out plus scale/vertical motion.
- Home story indicator: `22 → 7` width spring.

Compose implementations deliberately use `graphicsLayer` for transform-only animation to avoid avoidable layout work during presses and transitions.

## Visual/source parity

- Full 478-key iOS localization dictionary is ported for English, Russian, Uzbek Latin and Uzbek Cyrillic, with English fallback.
- Swift `%@` formatting is normalized to JVM `%s` without changing numeric format specifiers.
- iOS wordmarks, onboarding art, Care mark, Makkah fallback image and Flights Home artwork are copied from the source repository.
- The exact Home story clips are kept at the source 512×910 / 30 fps geometry and H.264 format, with a high-quality CRF 20 transcode so the sequential ZIP patches remain practical to upload from GitHub mobile. The clips are not cropped or downscaled; the UI also has a static fallback if a media patch is temporarily absent.

## Deliberate non-loss rule

Nothing removed from later iOS features is considered “ported” merely because a placeholder route exists. The parity manifest continues to track those Swift files until their Android implementation lands in later stages.
