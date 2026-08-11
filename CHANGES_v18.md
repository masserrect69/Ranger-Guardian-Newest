# Ford Guardian v18 changes

## Diagnostic correctness

- Corrected standard temperature, percentage and voltage decoding.
- Added a robust ELM/vLinker parser for compact `7E8...`/`7E9...` output, spaced frames and numbered ISO-TP responses.
- Added per-ECU Mode 01 capability discovery and polling filters.
- Added plausibility limits and freshness timestamps so stale or impossible values are not published as live data.
- Consolidated runtime PID keys and removed duplicate Ford profile DIDs.
- Added the supplied Ranger/6R80 custom definitions as opt-in experimental PIDs.
- Added stored, pending and permanent DTC scanning across responding ECUs.

## Bluetooth and polling

- Kept classic Bluetooth SPP for the vLinker FS CV304.
- Added secure SPP, insecure SPP and RFCOMM channel-one connection fallbacks.
- Fixed runtime Bluetooth permission handling on Android 12 and later.
- Added one shared `ObdViewModel` and one serialized adapter connection across all screens.
- Improved delayed-response handling, header switching, unsupported-PID backoff and poll-rate reporting.
- Applied the settings poll interval and experimental-PID toggle to the actual scheduler.

## UI and state

- Parameters now display live values instead of placeholders.
- Settings now show the actual build version and control the live poller.
- Alerts use fresh readings only; unverified Ford alerts are disabled by default.
- The dashboard score was renamed/reduced to a limited verified live-reading check rather than a broad vehicle-health diagnosis.
- Added DTC cards and scan/rescan controls.

## AI and security

- Removed direct OpenAI API calls and API-key storage from the Android app.
- Added a small server-side OpenAI Responses API backend.
- Fixed duplicate submission of the current chat message.
- Sends structured readings with units, freshness, source classification, ECU support and DTCs.
- Added request-size limits, history limits, output limits, rate limiting and optional app-token checking.
- Disabled Android backup and removed unused location/notification permissions.
- Moved release signing credentials out of source control.
- Removed any full VIN/calibration identifier from tests and packaged source.

## Not implemented as a claim

- Background monitoring was removed because the original service did not own a working Bluetooth/polling session.
- The source package was not turned into an APK in the repair environment because a complete Android SDK/Gradle dependency toolchain was unavailable. Build the project in Android Studio before installing it on the phone.
