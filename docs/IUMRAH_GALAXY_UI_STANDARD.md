# iumrah Galaxy UI Standard 1.0

Status: mandatory Android visual/interaction baseline.

Reference direction: Samsung One UI, especially the calm information density and tile language seen in Samsung Health and Samsung Reminder. This is an iumrah design system, not a clone of proprietary Samsung app code or assets.

## 1. Product feeling

The Android client must feel designed for a modern Galaxy device rather than ported from iOS or assembled from default Material components.

The target qualities are:

- calm, soft, precise surfaces;
- strong but restrained hierarchy;
- generous viewing area and reachable interaction area;
- immediate tactile feedback;
- no visual noise from unnecessary borders, pills or shadows;
- native system insets, system typography and Android navigation behavior;
- smooth interaction on 60/90/120 Hz displays.

## 2. Layout tokens

Use `IumrahGalaxyMetrics` from `core/design/IumrahTheme.kt`.

- screen horizontal margin: **24 dp**;
- normal content gap: **14 dp**;
- major section gap: **30 dp**;
- standard touch target: **48 dp**;
- main control height: **54 dp**;
- main CTA height: **56 dp**.

Do not invent arbitrary screen margins per page. Full-bleed photo/video surfaces are the exception.

## 3. Safe areas / edge-to-edge

`MainActivity` stays edge-to-edge. Every top-level screen must therefore consume Android system insets deliberately.

- root headers use `statusBarsPadding()`;
- full-screen detail pages must use a status-bar inset for their back/app-bar controls;
- fixed bottom actions use `navigationBarsPadding()`;
- never replace real insets with hard-coded `40–60 dp` top spacing.

A UI element touching or colliding with the status bar is a release blocker.

## 4. Geometry

Rounded corners are deliberate, not universal pills.

- small: **14 dp**;
- controls: **18 dp**;
- main buttons: **20 dp**;
- selection tiles: **22 dp**;
- cards: **26 dp**;
- large surfaces: **30 dp**;
- pills: only for true tags/statuses, not for every control.

Avoid `999.dp` unless the semantic component is genuinely circular/capsule-shaped.

## 5. Surface hierarchy

Light mode:

1. page: `#F5F5F7`;
2. card: white;
3. secondary/raised: soft neutral grey;
4. selected/high emphasis: graphite, not raw black everywhere.

Dark mode follows the same hierarchy with three distinct dark surface levels.

Rules:

- no heavy black outlines around ordinary inputs;
- use surface separation before shadows;
- shadows should normally be 0–3 dp;
- reserve dark filled surfaces for selected/high-emphasis states;
- do not make every card look floating.

## 6. Typography

Use system sans through the theme. Do not bundle a Samsung font.

Hierarchy:

- large page title: 31 sp / semi-bold;
- medium page title: 26 sp / semi-bold;
- section title: 20 sp / semi-bold;
- body: 16 sp;
- secondary body: 14 sp;
- labels: 11–15 sp.

Rules:

- one primary large title per viewing area;
- section headings must not compete with the page title;
- body copy should usually be normal weight;
- avoid chains of 24–34 sp bold headings in one scroll view;
- layouts must survive Android font scaling without clipping.

## 7. Inputs

Inputs use filled soft surfaces, rounded 18 dp, no persistent heavy outline.

- focus may change tone/cursor but should not create a thick black rectangle;
- leading icons use reduced contrast;
- labels and selected airport names must be readable without all-caps styling;
- text input work must never run expensive parsing/network code on the main thread.

## 8. Selection controls

Use equal-geometry tiles for route, flexibility and package options.

- unselected: neutral raised surface;
- selected: high-emphasis surface with explicit check state where useful;
- animate color/scale rather than replacing layout;
- every option must be fully visible; do not leave a clipped last item as an accidental carousel hint.

For four package levels, prefer a clean 2×2 tile grid on phone rather than a horizontally clipped row.

## 9. Counters

Traveler counters live in one grouped surface.

- row height around 68 dp;
- `+` and `−` visual buttons around 42 dp inside a 48 dp interaction philosophy;
- disabled state is visibly muted;
- value stays optically centered;
- separators are subtle.

## 10. Buttons

Main CTA:

- 56 dp height;
- 20 dp corner radius;
- semi-bold 16 sp label;
- 3 dp or less elevation;
- immediate press scale/tone response;
- fixed bottom CTA must respect navigation-bar inset.

Do not use an oversized black pill as the default button shape.

## 11. Motion

`IumrahMotion` defines the shared motion language.

Normal transitions use the One UI-style cubic curve:

`cubic-bezier(0.22, 0.25, 0.00, 1.00)`

Timing guidance:

- micro response: ~100–130 ms;
- selection/color: ~130–200 ms;
- page transition: ~180–250 ms;
- large reveal: spring / ~300+ ms only when spatially justified.

Rules:

- finger-down response must be immediate;
- do not animate whole-screen scale on every root tab change;
- prefer alpha/translation/scale/draw transforms over layout remeasurement;
- no decorative motion that delays a task.

## 12. Haptics

Use `IumrahHaptics` for selection, success, error and press feedback.

- do not stack two haptics for one action;
- selection changes may use selection feedback;
- ordinary press feedback stays subtle.

## 13. Bottom navigation

The bottom bar is a quiet system surface.

- no giant selected card around the entire tab;
- selected icon receives a small soft indicator capsule;
- labels remain small and secondary;
- navigation bar inset is always consumed.

## 14. Performance release gate

Visual quality includes frame pacing.

For every major screen before release:

- test a release/profile build on physical Android hardware;
- verify scroll while images are loading;
- verify keyboard opening/closing;
- verify repeated selection/counter taps;
- verify root-tab changes and back navigation;
- no network, JSON parsing or image transforms on the main thread;
- use stable lazy-list keys;
- avoid high-cost blur during scroll;
- avoid unnecessary recomposition from broad mutable state reads.

A debug build is not a valid smoothness benchmark.

## 15. Scope boundary

This design standard is presentation-only. It must not change:

- `https://iumrah.app` backend contracts;
- booking/account/hotel/flight APIs;
- pricing formulas or Android's temporary markup rule;
- route/open-jaw semantics;
- persisted journey data.

Any future visual patch that needs to alter one of those contracts must be split from the UI patch and reviewed separately.
