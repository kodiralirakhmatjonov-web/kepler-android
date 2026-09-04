# Stage 004 — Hotels parity

Ported from `HotelModels.swift`, `HotelCatalogService.swift`, `RemotePackageEngineClient.swift`, `HotelsHomeView.swift`, `HotelCard.swift` and the hotel-detail room/category surface.

Backend contracts remain shared:
- `GET /api/catalog/hotels?city=...`
- `GET /api/catalog/hotels/{id}`
- `GET /api/package/hotel/{id}/room-categories`
- `GET /api/package/hotel/{id}/pricing-sources`
- `GET /api/package/primary-hotel`

The raw external hotel supplier is not surfaced to the pilgrim; consumer identity stays `iumrah Hotels`, matching iOS.

Native card motion uses the Stage-003 iumrah motion system rather than Material default press animation. Hotel image paging and room-category selection are Compose-native.
