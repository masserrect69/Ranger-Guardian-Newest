# Ford Guardian v19 — validation results

Generated from the validated v18 source base. v19 changes are concentrated in the server-side Ranger Brain layer and Android chat request/source-display integration; OBD transport and decoder logic were not deliberately changed.

## Completed static validation
- `node --check backend/server.mjs`: PASS.
- `node --check backend/setup-knowledge.mjs`: PASS.
- `node --check backend/tools/generate-client-token.mjs`: PASS.
- `node --check backend/tools/fetch-public-ford-sources.mjs`: PASS.
- `backend/package.json` JSON parse: PASS.
- Android XML parse across `app/src`: PASS.
- Android version: `versionCode 19`, `versionName 1.0.19`: PASS.
- Whole-source privacy scan for raw `sk-` API keys, Haynes authenticated-session identifiers and full `MPB` VIN-like literals: PASS, none found.
- Android-source isolation scan for `OPENAI_API_KEY`, direct `api.openai.com` calls and raw keys: PASS, none found.
- Structural delimiter scan of changed Kotlin AI/chat files: PASS.
- v18 validated decoder/parser tests are retained. The only test-source edit is removal of a literal full-length VIN-shaped fixture while preserving manufacturer-prefix coverage.

## Build limitation
A complete Android APK build was not performed in this environment because the Android SDK/Gradle binary dependencies are not installed here. The source is prepared for Android Studio (JDK 17 / Android SDK Platform 34, consistent with v18). Build and device-level Bluetooth/OBD road testing must still occur on an Android development machine.

## Live OpenAI/knowledge-store limitation
No plaintext OpenAI API key is exposed to this workspace, by design. Therefore this environment did not create the user's live vector store or make a billable live API request. The included backend setup creates the vector store from `backend/knowledge/` once run on the user's backend machine with its server-side `OPENAI_API_KEY`.

## Haynes pointer validation — 12 Aug 2026
- Registered `c_15355` / `s_344759` as a pointer only.
- No Haynes session identifier is stored.
- No protected Haynes section text is bundled or claimed as indexed.

## Public Ford source fetch validation — 12 Aug 2026
- Ford Australia publicly lists PX Ranger MkII BEMMs for 2015MY and 2019MY.
- The fetch helper is restricted to HTTPS on `www.ford.com.au`, checks PDF content type and caps each file at 30 MB.
- A live fetch attempt from this build environment was blocked by sandbox DNS (`EAI_AGAIN`); this is an environment/network limitation, not an API or script parse failure. The Ford URLs were independently verified as current public Ford targets before packaging.
