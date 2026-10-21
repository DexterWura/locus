# Locus Privacy Policy

**Last updated:** 21 October 2026

Locus (“the app”) is a personal day-trail tracker for Android. This policy describes what data the app handles and how.

## Who we are

Locus has no Locus account and no cloud sync operated by the developer. Trails are stored on your phone. You may optionally link the phone to a company Device Management system by scanning a QR code they provide.

## Data Locus collects and stores on your device

- **Precise location** — GPS (and related location providers) points used to draw your day path, including time, accuracy, and speed when available.
- **Places you name** — optional pin labels (name, coordinates, radius, and optional note) you drop on the map or assign to stops.
- **App settings** — tracking on/off, tracking mode, sampling interval, onboarding completion.
- **Company link credentials** — if you scan a company QR, the server URL, device token, and asset name for that link (stored in encrypted preferences).
- **Lock credentials** — salted password and recovery hashes (not plaintext passwords). These never leave the device.

Trail points stay in the app’s private storage on your phone. The trail database is encrypted at rest (SQLCipher) with a key wrapped by the Android Keystore. Cloud backup of app data is disabled.

## Data that may leave the device

- **Map tiles** — When you view the map, the MapLibre client requests map imagery over HTTPS from OpenFreeMap (`tiles.openfreemap.org`). Tile requests reflect the map region you are viewing (which is usually near your location).
- **Company Device Management (optional)** — Only after you scan a company QR in Settings. While linked, Locus sends new location points (coordinates, time, and related accuracy/speed/bearing when available) over HTTPS to that company’s ERP so they can see the phone’s last fix and recent path. Unlinking stops further uploads. Passwords and recovery data are never uploaded.

Locus does not include advertising SDKs, analytics SDKs, or crash reporters that send personal data to third parties.

## Background location

If you choose **All day in background** in Settings, Locus may collect precise location while the app is closed or not in use, solely to continue recording your day trail. While using the app (the default mode) does not require background location. You can turn tracking off or switch modes at any time in Settings. If a company link is active, background-collected points may also be uploaded to that company.

## Foreground service notice

All-day background tracking uses an Android foreground service and shows an ongoing notification so you know location is being recorded.

## Retention and deletion

Data remains on the device until you delete it. Use **Delete all trail data** in Settings to remove recorded points and named places. Use **Unlink company** to stop sending locations to a linked ERP. Uninstalling the app also removes app-private data.

## Children

Locus is not directed at children under 13.

## Contact

For privacy questions about this app, open an issue on the project repository:  
https://github.com/DexterWura/locus

## Changes

We may update this policy when the app’s data practices change. The “Last updated” date at the top will change accordingly.
