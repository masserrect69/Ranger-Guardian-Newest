# Ford Guardian v19 — Ranger Brain

- Upgraded AI backend to a Ranger-specific evidence hierarchy.
- Default AI model is GPT-5.6 Sol with medium reasoning effort; model remains server-configurable.
- Added OpenAI File Search knowledge-base support and setup script.
- Added optional official-source web search restricted to Ford/Haynes domains.
- Added private vehicle history, historical DTCs and observed baselines from five supplied CSV sessions.
- Added Haynes `m_902` integration slot without storing the user's authenticated session token.
- Added Ford OEM public-reference/source manifest.
- Added source citations returned from backend and displayed in Android chat.
- Expanded app AI context with PID key/DID/header/mode/freshness and explicit operating-state fields.
- Kept Ford Mode 22 definitions experimental unless independently verified.
- Uses `store: false`; full VIN remains excluded; OpenAI API key remains server-only.
- Bumped Android version to 1.0.19 / versionCode 19.

## Haynes section pointer update — 12 Aug 2026
- Registered the user's exact Haynes `m_902` chapter/section pointer (`c_15355` / `s_344759`).
- Added explicit guardrails so an opaque Haynes URL cannot be treated as if its protected text had been indexed.
- Kept authentication/session identifiers out of source and generated packages.
- Added `npm run knowledge:fetch-public` to retrieve Ford Australia's public PX MkII 2015/2019 BEMM PDFs directly from Ford on the backend machine instead of redistributing the large OEM PDFs inside the source ZIP.
