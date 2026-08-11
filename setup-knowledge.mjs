import "dotenv/config";
import fs from "node:fs";
import path from "node:path";
import OpenAI from "openai";

const apiKey = process.env.OPENAI_API_KEY;
if (!apiKey) {
  console.error("OPENAI_API_KEY is required on the backend machine.");
  process.exit(1);
}

const openai = new OpenAI({ apiKey });
const knowledgeDir = path.resolve("knowledge");
const allowed = new Set([".pdf", ".txt", ".md", ".csv", ".json", ".html", ".docx", ".pptx"]);

function walk(dir) {
  if (!fs.existsSync(dir)) return [];
  const files = [];
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) files.push(...walk(full));
    else if (entry.isFile() && allowed.has(path.extname(entry.name).toLowerCase())) files.push(full);
  }
  return files;
}

const files = walk(knowledgeDir).filter((p) => !path.basename(p).startsWith("README"));
if (!files.length) {
  console.error("No indexable knowledge files found under ./knowledge");
  process.exit(1);
}

console.log(`Creating Ford Guardian vector store with ${files.length} file(s)...`);
const vectorStore = await openai.vectorStores.create({ name: "Ford Guardian Ranger Brain v19" });
const streams = files.map((p) => fs.createReadStream(p));
const batch = await openai.vectorStores.fileBatches.uploadAndPoll(vectorStore.id, { files: streams });

console.log(`Vector store: ${vectorStore.id}`);
console.log(`Upload status: ${batch.status}`);
if (batch.file_counts) console.log("File counts:", batch.file_counts);
console.log("\nPut this in backend/.env:");
console.log(`FORD_RANGER_VECTOR_STORE_ID=${vectorStore.id}`);
