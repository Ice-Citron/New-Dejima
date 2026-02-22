// Use CJS version of Paid.ai to avoid broken ESM tracing module (Node 24 compat)
import { createRequire } from "module";
const require = createRequire(import.meta.url);
const { PaidClient } = require("@paid-ai/paid-node");
import dotenv from "dotenv";
dotenv.config();

const PAID_API_KEY = process.env.PAID_AI_API_KEY ?? "";

// Lazy singleton — only create client when API key is present
let _client: InstanceType<typeof PaidClient> | null = null;
function getClient() {
  if (!_client) {
    if (!PAID_API_KEY || PAID_API_KEY === "your_paid_ai_key_here") {
      return null;
    }
    _client = new PaidClient({ token: PAID_API_KEY });
  }
  return _client;
}

export interface CostEvent {
  agentId: string;
  model: string;
  inputTokens: number;
  outputTokens: number;
  estimatedCostUsd: number;
  timestamp: string;
}

export interface RevenueEvent {
  agentId: string;
  source: string; // "android_app" | "saas" | "trading"
  amountUsd: number;
  description: string;
  timestamp: string;
}

// Track an API cost event via Paid.ai
export async function trackCost(event: CostEvent) {
  const client = getClient();
  if (!client) {
    console.log(`[Paid.ai] (no key) Cost: $${event.estimatedCostUsd.toFixed(4)} for ${event.model}`);
    return;
  }
  try {
    (client as any).usage.record({
      event_name: "api_cost",
      external_customer_id: event.agentId,
      external_product_id: "agent-compute",
      data: {
        model: event.model,
        input_tokens: event.inputTokens,
        output_tokens: event.outputTokens,
        cost_usd: event.estimatedCostUsd,
      },
    });
    await (client as any).usage.flush();
    console.log(`[Paid.ai] Cost tracked: $${event.estimatedCostUsd.toFixed(4)} for ${event.model}`);
  } catch (err) {
    console.error("[Paid.ai] Cost tracking error:", err);
  }
}

// Track a revenue event (agent earned money)
export async function trackRevenue(event: RevenueEvent) {
  const client = getClient();
  if (!client) {
    console.log(`[Paid.ai] (no key) Revenue: $${event.amountUsd.toFixed(2)} from ${event.source}`);
    return;
  }
  try {
    (client as any).usage.record({
      event_name: "revenue_earned",
      external_customer_id: event.agentId,
      external_product_id: event.source,
      data: {
        amount_usd: event.amountUsd,
        description: event.description,
      },
    });
    await (client as any).usage.flush();
    console.log(`[Paid.ai] Revenue tracked: $${event.amountUsd.toFixed(2)} from ${event.source}`);
  } catch (err) {
    console.error("[Paid.ai] Revenue tracking error:", err);
  }
}
