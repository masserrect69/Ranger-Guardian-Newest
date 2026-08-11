# Ford Guardian Ranger Brain — Render deployment

Use the full backend bundle first to create the private OpenAI vector store.
This deployment bundle intentionally excludes the licensed workshop PDF.

Render settings:
- Runtime: Node
- Node version: 22
- Build command: npm install
- Start command: npm start
- Health check: /v1/health
- Do not hard-code PORT; Render supplies PORT.

Required secrets:
- OPENAI_API_KEY
- FORD_RANGER_VECTOR_STORE_ID
- FORD_GUARDIAN_CLIENT_TOKEN

Recommended settings:
- OPENAI_MODEL=gpt-5.1
- OPENAI_REASONING_EFFORT=medium
- OFFICIAL_WEB_SEARCH=true
- OFFICIAL_WEB_SEARCH_CONTEXT=medium
- MAX_REQUESTS_PER_MINUTE=30
- MAX_HISTORY_MESSAGES=20
- MAX_OUTPUT_TOKENS=2200
