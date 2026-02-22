import { readFileSync, writeFileSync, existsSync, mkdirSync } from "fs";
import { dirname, join } from "path";
import { fileURLToPath } from "url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const STORE_PATH = join(__dirname, "..", "data", "events.json");

export interface TrackingEvent {
  type: "cost" | "revenue" | "build";
  agentId: string;
  timestamp: string;
  data: Record<string, unknown>;
}

let events: TrackingEvent[] = [];

function ensureDir() {
  const dir = dirname(STORE_PATH);
  if (!existsSync(dir)) mkdirSync(dir, { recursive: true });
}

export function loadEvents(): TrackingEvent[] {
  ensureDir();
  if (!existsSync(STORE_PATH)) return [];
  try {
    events = JSON.parse(readFileSync(STORE_PATH, "utf-8"));
  } catch {
    events = [];
  }
  return events;
}

export function addEvent(event: TrackingEvent) {
  events.push(event);
  ensureDir();
  writeFileSync(STORE_PATH, JSON.stringify(events, null, 2));
}

export function getEvents(): TrackingEvent[] {
  return events;
}

export function getAgentSummaries() {
  const agents = new Map<
    string,
    { cost: number; revenue: number; tokens: number; builds: number; lastModel: string }
  >();
  for (const evt of events) {
    const id = evt.agentId;
    if (!agents.has(id))
      agents.set(id, { cost: 0, revenue: 0, tokens: 0, builds: 0, lastModel: "unknown" });
    const a = agents.get(id)!;
    if (evt.type === "cost") {
      a.cost += (evt.data.costUsd as number) || 0;
      a.tokens +=
        ((evt.data.inputTokens as number) || 0) + ((evt.data.outputTokens as number) || 0);
      if (evt.data.model) a.lastModel = evt.data.model as string;
    }
    if (evt.type === "revenue") a.revenue += (evt.data.amountUsd as number) || 0;
    if (evt.type === "build") a.builds += 1;
  }
  return Object.fromEntries(agents);
}
