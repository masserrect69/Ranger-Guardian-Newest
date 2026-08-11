import "dotenv/config";
import http from "node:http";
import crypto from "node:crypto";
import OpenAI from "openai";

const port = Number(process.env.PORT || 8787);
const apiKey = process.env.OPENAI_API_KEY;
const model = process.env.OPENAI_MODEL || "gpt-5.1";
const reasoningEffort = process.env.OPENAI_REASONING_EFFORT || "medium";
const vectorStoreId = process.env.FORD_RANGER_VECTOR_STORE_ID || "";
const requiredClientToken = process.env.FORD_GUARDIAN_CLIENT_TOKEN || "";
const officialWebSearch = String(process.env.OFFICIAL_WEB_SEARCH || "true").toLowerCase() === "true";
const officialWebSearchContext = process.env.OFFICIAL_WEB_SEARCH_CONTEXT || "medium";
const maxRequestsPerMinute = Number(process.env.MAX_REQUESTS_PER_MINUTE || 30);
const maxHistoryMessages = Number(process.env.MAX_HISTORY_MESSAGES || 20);
const maxOutputTokens = Number(process.env.MAX_OUTPUT_TOKENS || 2200);
const rateBuckets = new Map();

if (!apiKey) {
  console.error("OPENAI_API_KEY is required on the backend.");
  process.exit(1);
}

const openai = new OpenAI({ apiKey });

const instructions = `
You are Ford Guardian Ranger Brain, a careful Ford Ranger diagnostic reasoning assistant.

PRIMARY VEHICLE
The private knowledge base contains the configured user's vehicle profile and observed history. Apply exact-year/engine/transmission facts only when the source says they match. The primary configured vehicle is a 2018 Australian-market PX MkII Ford Ranger with the 3.2 L five-cylinder Duratorq/Puma diesel and 6R80 automatic transmission.

SOURCE HIERARCHY
1. Fresh live OBD readings and DTCs in the current request.
2. Private vehicle history and repeated observed baselines from file_search.
3. Ford OEM owner/service/body documentation retrieved from file_search or official Ford web search.
4. User-provided licensed manuals such as Haynes files in the private vector store.
5. General mechanical reasoning.
Never silently convert lower-confidence material into a Ford specification.

DIAGNOSTIC METHOD
- Establish operating state before interpreting a number: coolant temperature, RPM, load, speed, gear when known, MAP/airflow, battery voltage, DPF regeneration state and freshness.
- Prefer trends, repeatability, correlated signals and commanded-vs-actual relationships over one isolated sample.
- Treat asynchronous scan-tool samples carefully; do not calculate a precise dynamic tracking error unless timestamps/sampling support it.
- Separate symptom, evidence, hypotheses and confirmation tests.
- Rank likely causes and explain what would support or weaken each one.
- Prefer safe, low-invasiveness confirmation checks before component replacement.
- If important data are missing, name the specific readings/tests that would improve confidence.
- Never claim to have physically inspected the vehicle.

PID / DATA INTEGRITY
- SAE Mode 01 data marked verified_mode_01 is the trusted live-data tier only when fresh and advertised as supported by the responding ECU.
- Ford Mode 22 data marked experimental_ford_mode_22 stays experimental unless the vehicle profile or a trusted source explicitly records independent validation on this exact vehicle.
- Do not use one experimental PID as the sole basis for a repair decision.
- Never invent a Ford DID/PID, formula, CAN header, connector pin, wire colour, torque specification, threshold, DTC sub-type, workshop procedure or service bulletin.
- A plausibility range in app code is not automatically a Ford service limit.
- A Haynes section pointer or URL is not evidence of the section contents. Use Haynes repair facts only when the relevant exported/licensed content is actually present in file_search results.
- The private knowledge base now contains a user-provided Ford Ranger workshop-manual compilation. It includes multiple engines, build dates and emissions markets. Apply MANUAL_APPLICABILITY_GUARDRAILS.md before treating a retrieved passage as applicable.
- For the primary 2018 vehicle prefer 3.2L Duratorq/Puma + 6R80 + Vehicles Built From: 17-08-2015 content. Prefer RHD only after the build-date match is satisfied.
- Known source defect: workshop-manual PDF page 1512 is headed as a 3.2L turbo overview but contains generic EcoBoost twin-turbo text. Do not apply that twin-turbo paragraph to the 3.2L Puma.
- Values in observed-baseline files are observations from this vehicle under particular sessions, not manufacturer specifications or guaranteed normal ranges.
- Clearly flag sensors that historical data identify as invalid/unreliable (for example a stuck sentinel value).

RANGER-SPECIFIC REASONING
When relevant, consider the relationships among turbo/VGT control, intake leaks, exhaust leaks before the turbine, MAF/MAP, EGR command/position, DPF pressure/soot/regeneration, EGTs, rail-pressure demand/actual, injector balance, transmission temperature and charging voltage. Account for documented vehicle modifications because they can change expected EGR/airflow/exhaust behaviour, but do not recommend defeating emissions or safety systems.

WEB SEARCH
Use web search only when the private knowledge base does not contain the Ford-specific fact needed, or when current Ford information is relevant. Web access is domain-restricted by the server to Ford and Haynes. Treat model-year/market applicability explicitly. A current Ranger page is not proof of a 2018 PX MkII procedure.

OUTPUT
Lead with the conclusion and confidence. Then give the evidence and next checks in practical order. Use the units supplied by the app. Cite retrieved sources when the API supplies citations. Say "needs verification" rather than guessing a Ford-specific fact.
`.trim();

function sendJson(res, status, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Content-Length": Buffer.byteLength(body),
    "Cache-Control": "no-store"
  });
  res.end(body);
}

async function readJson(req) {
  const chunks = [];
  let size = 0;
  for await (const chunk of req) {
    size += chunk.length;
    if (size > 1_000_000) throw new Error("Request body too large");
    chunks.push(chunk);
  }
  return JSON.parse(Buffer.concat(chunks).toString("utf8"));
}

function authorized(req) {
  if (!requiredClientToken) return true;
  return req.headers.authorization === `Bearer ${requiredClientToken}`;
}

function rateLimited(req) {
  const now = Date.now();
  const key = req.socket.remoteAddress || "unknown";
  const bucket = rateBuckets.get(key) || [];
  const recent = bucket.filter((timestamp) => now - timestamp < 60_000);
  if (recent.length >= maxRequestsPerMinute) {
    rateBuckets.set(key, recent);
    return true;
  }
  recent.push(now);
  rateBuckets.set(key, recent);
  return false;
}

function cleanHistory(history) {
  if (!Array.isArray(history)) return [];
  return history
    .filter((m) => m && ["user", "assistant"].includes(m.role) && typeof m.content === "string")
    .slice(-maxHistoryMessages)
    .map((m) => ({ role: m.role, content: m.content.slice(0, 10_000) }));
}

function sanitizeDiagnosticContext(body) {
  const readings = Array.isArray(body.readings) ? body.readings.slice(0, 180) : [];
  const dtcs = Array.isArray(body.dtcs) ? body.dtcs.slice(0, 80) : [];
  return {
    vehicle: body.vehicle && typeof body.vehicle === "object" ? body.vehicle : null,
    operatingState: body.operatingState && typeof body.operatingState === "object" ? body.operatingState : null,
    supportedEcus: body.supportedEcus && typeof body.supportedEcus === "object" ? body.supportedEcus : {},
    readings,
    dtcs
  };
}

function stableSafetyIdentifier(req) {
  const seed = requiredClientToken || req.socket.remoteAddress || "ford-guardian-private";
  return crypto.createHash("sha256").update(seed).digest("hex").slice(0, 32);
}

function extractCitations(response) {
  const result = [];
  const seen = new Set();
  for (const item of response.output || []) {
    if (item?.type !== "message") continue;
    for (const part of item.content || []) {
      for (const annotation of part.annotations || []) {
        if (annotation.type === "url_citation" && annotation.url) {
          const key = `url:${annotation.url}`;
          if (!seen.has(key)) {
            seen.add(key);
            result.push({
              kind: "official_web",
              title: annotation.title || annotation.url,
              url: annotation.url
            });
          }
        } else if (annotation.type === "file_citation") {
          const key = `file:${annotation.file_id || annotation.filename}`;
          if (!seen.has(key)) {
            seen.add(key);
            result.push({
              kind: "knowledge_file",
              title: annotation.filename || "Ranger knowledge file",
              fileId: annotation.file_id || null
            });
          }
        }
      }
    }
  }
  return result.slice(0, 12);
}

function toolsForRequest() {
  const tools = [];
  if (vectorStoreId) {
    tools.push({
      type: "file_search",
      vector_store_ids: [vectorStoreId],
      max_num_results: 10
    });
  }
  if (officialWebSearch) {
    tools.push({
      type: "web_search",
      search_context_size: officialWebSearchContext,
      filters: {
        allowed_domains: ["ford.com.au", "fordservicecontent.com", "haynes.com"]
      },
      user_location: {
        type: "approximate",
        country: "AU",
        timezone: "Australia/Sydney"
      }
    });
  }
  return tools;
}

const server = http.createServer(async (req, res) => {
  try {
    if (req.method === "GET" && (req.url === "/health" || req.url === "/v1/health")) {
      return sendJson(res, 200, {
        ok: true,
        model,
        knowledgeBase: Boolean(vectorStoreId),
        officialWebSearch,
        keyLocation: "server_only"
      });
    }

    if (req.method !== "POST" || req.url !== "/v1/diagnose") {
      return sendJson(res, 404, { error: "Not found" });
    }
    if (!authorized(req)) return sendJson(res, 401, { error: "Invalid app token" });
    if (rateLimited(req)) return sendJson(res, 429, { error: "Too many requests" });

    const body = await readJson(req);
    const message = String(body.message || "").trim();
    if (!message) return sendJson(res, 400, { error: "message is required" });
    if (message.length > 20_000) return sendJson(res, 400, { error: "message is too long" });

    const diagnosticContext = sanitizeDiagnosticContext(body);
    const history = cleanHistory(body.history);
    const input = [
      ...history,
      {
        role: "user",
        content: `${message}\n\nCURRENT FORD GUARDIAN DIAGNOSTIC CONTEXT (JSON):\n${JSON.stringify(diagnosticContext)}`
      }
    ];

    const request = {
      model,
      instructions,
      input,
      tools: toolsForRequest(),
      max_output_tokens: maxOutputTokens,
      store: false,
      safety_identifier: stableSafetyIdentifier(req)
    };

    if (/^(gpt-5|o\d)/i.test(model) && reasoningEffort) {
      request.reasoning = { effort: reasoningEffort };
    }
    if (vectorStoreId) request.include = ["file_search_call.results"];
    if (!request.tools.length) delete request.tools;

    const response = await openai.responses.create(request);
    const sources = extractCitations(response);

    return sendJson(res, 200, {
      answer: response.output_text || "",
      responseId: response.id,
      model,
      knowledgeBaseUsed: Boolean(vectorStoreId),
      officialWebSearchEnabled: officialWebSearch,
      sources
    });
  } catch (error) {
    console.error(error);
    const status = Number.isInteger(error?.status) ? error.status : 500;
    const safeStatus = [400, 401, 403, 404, 429].includes(status) ? status : 500;
    return sendJson(res, safeStatus, {
      error: safeStatus === 500 ? "Diagnostic service failed" : (error.message || "Request failed")
    });
  }
});

server.listen(port, "0.0.0.0", () => {
  console.log(`Ford Guardian Ranger Brain v19 listening on port ${port}`);
  console.log(`Model: ${model}`);
  console.log(`Knowledge vector store: ${vectorStoreId || "not configured"}`);
  console.log(`Official Ford/Haynes web search: ${officialWebSearch ? "enabled" : "disabled"}`);
});
