# Stage 005 — Trip + Flights parity

Source of truth: `beta-iumrah-app-main 7.zip`.

## Ported in this stage
- `TripDraft`, journey scope, arrival airport, date flexibility/weekend behavior and room/traveler constraints.
- Airport search through the existing `https://iumrah.app` API.
- Fare-calendar data contracts.
- Ignav journey provider through `/api/package/flights/search`.
- One complete outbound + return/open-jaw itinerary is represented as one `LiveFlightJourneyCandidate`.
- `fare_scope=total_party` remains mandatory for displayable Ignav candidates.
- The complete journey fare is carried once; Android must never add an outbound one-way fare to a return one-way fare.
- Hotel selection flows Makkah -> Madinah (when required) -> flight search.
- Trip Builder and flight results are native Compose screens and are connected to the real root navigation; they are not stage placeholders.

## Native motion acceptance
Flight/hotel choice cards use the iumrah press spring rather than Material ripple defaults. Selection border/scale changes use the shared `IumrahMotion.selection` spring. Root route changes retain the 0.985 -> 1.0 / 1.0 -> 1.015 motion language from iOS.

## Intentionally later
Final package generation, checkout and booking creation are Stage 006/007 work. Booking and Care root tabs therefore remain placeholders at this stage; Trip Builder, Hotel Selection, Hotels and Flights do not.

## Test pricing exception
Android beta package markup remains **20%**. All other pricing rules remain parity-controlled.
