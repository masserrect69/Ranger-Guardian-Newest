# Ford Guardian Ranger Brain v19 backend

This backend keeps the OpenAI key off the phone and gives Ford Guardian three evidence layers:
1. fresh live OBD/DTC context from the app;
2. a private OpenAI vector store containing the configured Ranger's history/baselines and user-supplied manuals;
3. optional OpenAI web search restricted to Ford Australia, Ford Service Content and Haynes public pages.

## Setup
Requires Node.js 22+.

```bash
cd backend
npm install
cp .env.example .env
```

Edit `.env` on the backend machine and set `OPENAI_API_KEY`. Generate a private app token with:

```bash
npm run token:generate
```

Put that output into `FORD_GUARDIAN_CLIENT_TOKEN` and enter the same token in Ford Guardian's AI Server settings.

## Build the private Ranger knowledge store
The included knowledge folder already contains:
- vehicle profile/history
- verified historical DTC list
- PID trust rules
- summaries of five supplied 2026 live-data sessions
- Ford public-reference notes/source manifest
- a Haynes manual slot

Add any legitimately exported Haynes/official workshop files under `knowledge/user_manuals/`, then:

```bash
npm run knowledge:setup
```

Copy the printed `FORD_RANGER_VECTOR_STORE_ID=vs_...` value into `.env`.

## Run
```bash
npm start
```

Health check:
```bash
curl http://localhost:8787/v1/health
```

For the Android emulator, the debug build may use `http://10.0.2.2:8787`. A physical phone should use an HTTPS deployment. Do not expose the backend to the internet without the client token and normal hosting protections.

## Privacy/security
- `OPENAI_API_KEY` stays server-side.
- Responses use `store: false`.
- The app does not send the full VIN.
- A hashed server-side safety identifier is used instead of PII.
- Vector-store knowledge persists until you delete it from OpenAI; it is therefore appropriate only for material you choose to store there.

## Fetch Ford's public PX MkII reference PDFs

The package keeps large OEM PDFs out of the ZIP. On the backend machine you can fetch Ford Australia's public PX Ranger MkII Body & Equipment Mounting Manuals directly from Ford:

```bash
npm run knowledge:fetch-public
```

The fetcher is hard-limited to HTTPS on `www.ford.com.au`, verifies PDF content type, and caps each download at 30 MB. Then run `npm run knowledge:setup` so those files are included in the private Ranger vector store.
