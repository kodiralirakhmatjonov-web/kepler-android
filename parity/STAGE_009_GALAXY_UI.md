# Stage 009 — Galaxy UI foundation

This stage establishes the Android-only iumrah Galaxy UI standard.

## Changed

- shared color, shape, spacing and typography tokens;
- One UI-style shared motion curve and calmer page transitions;
- shared press feedback, haptics, buttons and back control;
- real status/navigation-bar inset handling for primary screens;
- root header and bottom navigation redesign;
- complete Trip Builder presentation redesign;
- 24 dp page rhythm on root screens and major detail flows;
- package level selector changed from a clipped horizontal row to a complete 2×2 grid;
- traveler counters grouped into one Samsung Reminder-inspired surface;
- date and route selections moved to equal soft tiles;
- departure/return date labels now use existing localized strings instead of hard-coded English.

## Explicitly unchanged

- backend URL and request contracts;
- D1/R2/server architecture;
- hotel, flight and booking data logic;
- pricing and markup logic;
- journey state model;
- route semantics.

See `docs/IUMRAH_GALAXY_UI_STANDARD.md` for the mandatory visual baseline.
