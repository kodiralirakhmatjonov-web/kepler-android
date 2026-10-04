# iumrah Android · FCM push setup

The Android client now contains the complete FCM token lifecycle, notification service,
backend device registration and push deep-routing. The repository deliberately does not
invent Firebase project identifiers.

Create/register the Android app `com.iumrah.beta` in the Firebase project that owns iumrah
push delivery, then copy `firebase.properties.example` to `firebase.properties` and fill:

- `IUMRAH_FIREBASE_API_KEY`
- `IUMRAH_FIREBASE_APP_ID`
- `IUMRAH_FIREBASE_PROJECT_ID`
- `IUMRAH_FIREBASE_SENDER_ID`

The same values can instead be supplied as Gradle properties or environment variables.
No service-account JSON or private server key belongs in the mobile repository.

After configuration, the app will:

1. initialize Firebase at application launch;
2. request Android notification permission once after onboarding on Android 13+;
3. obtain and persist the case-sensitive FCM registration token;
4. register that token with iumrah Signal and each booking;
5. receive foreground/data notifications through `IumrahFirebaseMessagingService`;
6. refresh booking/Signal state after a push event;
7. route notification taps to Home, Hotels, Booking, Care or Account like iOS.

The server behind `/api/catalog/hotels/client/notifications/devices` and
`/api/catalog/hotels/client/push/devices` must have an FCM sender/provider configured for
Android delivery. The client exposes server readiness in Account → Notifications.
