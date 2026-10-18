# Locus

Private, on-device day trail tracking for Android.

Locus records where you go and when, draws the path on a map, and keeps every coordinate on the phone. There is no account, no sync, and no cloud backup of your trail.

Inspired by the calm full-screen map and bottom sheet of Google Find My Device — without remote lock, ring, or web locate. This phone only watches itself.

## Features

- **Today** — live path on a MapLibre map, status sheet (Moving / Still / Paused), compass heading
- **Compass** — device direction (N / NE / E …) from the phone’s sensors
- **History** — day list with distance, time span, and canvas path thumbnails
- **Day detail** — animated path plus trip/stop timeline; name places locally
- **Tracking toggle** — turn path recording on/off in Settings
- **Tracking modes** — while using the app (no sticky notice) or all-day background (ongoing notice + disclosure)
- **Encrypted at rest** — SQLCipher trail DB, encrypted prefs, Android Keystore; cloud backup disabled
- **Local lock** — password set during onboarding; stays unlocked ~10 minutes after you leave the app
- **Local recovery** — security questions and a one-time recovery phrase (nothing is emailed)
- **Battery-aware sampling** — ~5s while moving; relaxes when still to cut heat

## Privacy

| Stays on device | May use the network |
| --- | --- |
| GPS points, places you name, password hashes, recovery data | Map tiles (OpenFreeMap pictures of the world) |

**Privacy policy:** [PRIVACY.md](PRIVACY.md) · also in-app under Settings  
**Play Data safety notes:** [DATA_SAFETY.md](DATA_SAFETY.md)

Passwords and recovery answers are stored as salted PBKDF2 hashes. The recovery phrase is shown once at setup and never again.

If you forget your password **and** cannot answer the security questions **and** lose the recovery phrase, Locus cannot reset access for you — there is no server to ask.

## Stack

- Kotlin, Jetpack Compose, Material 3
- MapLibre + [OpenFreeMap](https://openfreemap.org/) Liberty style (no API key)
- Room + SQLCipher (trail), EncryptedSharedPreferences (settings + auth)
- Platform `LocationManager` (no Play Services Geocoder)

minSdk 29 · targetSdk 36

## Build

Open the project in Android Studio, or from the repo root:

```bash
./gradlew assembleDebug
./gradlew test
```

### Play Store release

1. Copy `keystore.properties.example` → `keystore.properties` and point it at your upload keystore (file is gitignored).
2. Build the App Bundle:

```bash
./gradlew bundleRelease
```

3. Upload `app/build/outputs/bundle/release/app-release.aab` to Play Console.
4. Complete declarations using [DATA_SAFETY.md](DATA_SAFETY.md) and set the privacy policy URL to the published [PRIVACY.md](PRIVACY.md).

## Permissions

| Permission | Why |
| --- | --- |
| Precise location | Draw the trail (requested at onboarding) |
| Background location | Optional — only when enabling all-day mode, after in-app disclosure |
| Notifications | Ongoing notice while all-day tracking runs |

## Project layout

```
app/src/main/java/com/whereami/locus/
  auth/          password, recovery, session lock
  data/          Room, encrypted prefs, repository
  domain/        models, PathGeometry
  presentation/  ViewModels
  tracking/      foreground service, LocationManager
  ui/            Compose screens, map, theme
  privacy/       privacy policy URL
```

## License

Personal project. Use and modify freely for your own device.
