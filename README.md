# Locus

Private, on-device day trail tracking for Android.

Locus records where you go and when, draws the path on a map, and keeps every coordinate on the phone. There is no account, no sync, and no cloud backup of your trail.

Inspired by the calm full-screen map and bottom sheet of Google Find My Device — without remote lock, ring, or web locate. This phone only watches itself.

## Features

- **Today** — live path on a MapLibre map, status sheet (Moving / Still / Paused), tracking switch
- **History** — day list with distance, time span, and canvas path thumbnails
- **Day detail** — animated path plus trip/stop timeline; name places locally
- **Local lock** — password set during onboarding; app locks when it leaves the foreground
- **Local recovery** — security questions and a one-time recovery phrase (nothing is emailed)
- **Battery-aware sampling** — ~5s / 8m while moving; relaxes when still

## Privacy

| Stays on device | May use the network |
| --- | --- |
| GPS points, places you name, password hashes, recovery data | Map tiles (OpenFreeMap pictures of the world) |

Passwords and recovery answers are stored as salted PBKDF2 hashes. The recovery phrase is shown once at setup and never again.

If you forget your password **and** cannot answer the security questions **and** lose the recovery phrase, Locus cannot reset access for you — there is no server to ask.

## Stack

- Kotlin, Jetpack Compose, Material 3
- MapLibre + [OpenFreeMap](https://openfreemap.org/) Liberty style (no API key)
- Room (trail), DataStore (settings + auth)
- Platform `LocationManager` (no Play Services Geocoder)

minSdk 29 · targetSdk 36

## Build

Open the project in Android Studio, or from the repo root:

```bash
./gradlew assembleDebug
```

Install the debug APK from `app/build/outputs/apk/debug/`.

Unit tests:

```bash
./gradlew test
```

## Permissions

| Permission | Why |
| --- | --- |
| Precise location | Draw the trail |
| Background location | Keep recording with the screen off |
| Notifications | Ongoing “tracking is active” notice |
| Battery optimization (optional) | Some OEMs pause GPS apps aggressively |

## Project layout

```
app/src/main/java/com/whereami/locus/
  auth/          password, recovery, session lock
  data/          Room, DataStore, repository
  domain/        models, PathGeometry
  presentation/  ViewModels
  tracking/      foreground service, LocationManager
  ui/            Compose screens, map, theme
```

## License

Personal project. Use and modify freely for your own device.
