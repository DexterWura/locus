# Play Console — Data safety & declarations (Locus)

Use this as the source of truth when filling Play Console forms. Keep it aligned with `PRIVACY.md` and the app code.

## Privacy policy URL

https://github.com/DexterWura/locus/blob/master/PRIVACY.md

Also linked in-app: Settings → Privacy policy.

## Data safety form (summary)

| Data type | Collected? | Shared? | Purpose | Ephemeral? | Required? |
| --- | --- | --- | --- | --- | --- |
| Precise location | Yes | Optional† | App functionality (day trail); optional company Device Management | No | Yes for tracking |
| App activity / settings | Yes (local) | No | App functionality | No | No |
| Password / recovery hashes | Yes (local) | No | App functionality / account (device lock) | No | Yes after onboarding |

\*Map tile HTTPS requests to OpenFreeMap reveal the map region being viewed (typically near the user). Declare **location** as collected; for “shared with third parties,” treat tile CDN usage per your Console guidance — Locus does not sell data and has no ads/analytics SDKs. Prefer declaring that location is used for app functionality and that network map tiles may be requested.

†Only after the user scans a company Device Management QR. New points are sent to that company’s ERP while linked. Unlink stops sharing.

**Encryption:** Data encrypted in transit (HTTPS tiles). Data encrypted at rest on device (SQLCipher + Keystore / EncryptedSharedPreferences).

**Deletion:** Users can delete trail data in Settings; uninstall removes app-private storage. No account deletion flow (no cloud account).

**Children:** Not designed for children; no ads.

## Sensitive permissions

### Precise location
Core feature: draw the on-device day trail.

### Background location
Only for optional **All day in background** mode. Default is foreground-only. In-app prominent disclosure appears before the system permission. Declaration video should show: Settings → All day → disclosure → permission → ongoing notification while tracking.

### Notifications
Required for the location foreground service notice in background mode.

### Foreground service — Location
`TrackingService` with `foregroundServiceType="location"`. Shown only in background tracking mode.

### Battery optimization
**Not requested.** `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` was removed for Play policy compliance.

## Release checklist

1. `./gradlew assembleRelease` (or `bundleRelease`) with `keystore.properties` present.
2. Upload AAB to Play Console; use Play App Signing.
3. Verify 16 KB page-size native libs (APK Analyzer / alignment tool) for SQLCipher and MapLibre `.so` files before targeting devices that enforce the requirement.
4. Complete Data safety, Location, and Foreground service declarations using this doc.
5. Content rating questionnaire; target audience 18+ recommended for continuous location apps if asked.
6. Store listing: feature graphic, screenshots, short/full description matching privacy claims.
