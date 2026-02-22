/**
 * New Dejima — Paid.ai Deep Integration
 *
 * Uses the PROPER Paid.ai SDK v0.5.1 APIs:
 *   - client.usage.usageRecordBulkV2()  for signal recording
 *   - client.customers.*                for customer management
 *   - client.products.*                 for product registration
 *   - client.orders.*                   for billing periods
 *   - client.traces.*                   for cost trace queries
 *
 * Paid.ai is the MAIN SPONSOR — we use every feature we can.
 */
import { createRequire } from "module";
const require = createRequire(import.meta.url);
const { PaidClient } = require("@paid-ai/paid-node");
import dotenv from "dotenv";
import crypto from "crypto";
dotenv.config();

const PAID_API_KEY = process.env.PAID_AI_API_KEY ?? "";

// ── Singleton client ──────────────────────────────────────────────
let _client: any = null;
function getClient(): any {
  if (!_client) {
    if (!PAID_API_KEY || PAID_API_KEY === "your_paid_ai_key_here") return null;
    _client = new PaidClient({ token: PAID_API_KEY });
  }
  return _client;
}

export function isPaidConfigured(): boolean {
  return !!getClient();
}

// ── Types ─────────────────────────────────────────────────────────
export interface CostEvent {
  agentId: string;
  model: string;
  inputTokens: number;
  outputTokens: number;
  estimatedCostUsd: number;
  timestamp: string;
  runId?: string;
  cacheRead?: number;
  cacheWrite?: number;
  durationMs?: number;
}

export interface RevenueEvent {
  agentId: string;
  source: string;
  amountUsd: number;
  description: string;
  timestamp: string;
}

export interface BuildEvent {
  agentId: string;
  appName: string;
  success: boolean;
  apkSize?: number;
  runId?: string;
}

// ── Customer management (New Dejima = the customer) ───────────────
const CUSTOMER_EXTERNAL_ID = "new-dejima-system";
const PRODUCT_EXTERNAL_ID = "dejima-agent-compute";

let _setupDone = false;

/**
 * Ensure the New Dejima customer + product exist in Paid.ai.
 * Idempotent — safe to call multiple times.
 */
export async function ensurePaidSetup(): Promise<{ customerId?: string; productId?: string }> {
  const client = getClient();
  if (!client) return {};
  if (_setupDone) return {};

  try {
    // 1. Ensure customer exists (New Dejima = the AI system that incurs costs)
    let customer: any;
    try {
      customer = await client.customers.getByExternalId(CUSTOMER_EXTERNAL_ID);
      console.log(`[Paid.ai] Customer found: ${customer.name} (${customer.id})`);
    } catch {
      customer = await client.customers.create({
        name: "New Dejima AI System",
        externalId: CUSTOMER_EXTERNAL_ID,
      });
      console.log(`[Paid.ai] Customer created: ${customer.name} (${customer.id})`);
    }

    // 2. Ensure product exists (agent-compute = the thing that costs money)
    let product: any;
    try {
      product = await client.products.getByExternalId(PRODUCT_EXTERNAL_ID);
      console.log(`[Paid.ai] Product found: ${product.name} (${product.id})`);
    } catch {
      product = await client.products.create({
        name: "Dejima Agent Compute",
        description:
          "AI agent compute costs — Claude Opus, Gemini, etc. Each signal = one LLM call with token counts and cost.",
        externalId: PRODUCT_EXTERNAL_ID,
        type: "agent",
      });
      console.log(`[Paid.ai] Product created: ${product.name} (${product.id})`);
    }

    // 3. Ensure an active order exists for this billing period
    try {
      const orders = await client.orders.list();
      const activeOrder = (orders || []).find(
        (o: any) => o.customer?.externalId === CUSTOMER_EXTERNAL_ID || o.customerId === customer.id
      );
      if (!activeOrder) {
        const now = new Date();
        const endDate = new Date(now);
        endDate.setMonth(endDate.getMonth() + 1);
        const order = await client.orders.create({
          customerExternalId: CUSTOMER_EXTERNAL_ID,
          name: `Dejima Hackathon — ${now.toISOString().slice(0, 10)}`,
          description: "HackEurope Paris 2026 — AI agent cost tracking period",
          startDate: now.toISOString().slice(0, 10),
          endDate: endDate.toISOString().slice(0, 10),
          currency: "USD",
        });
        console.log(`[Paid.ai] Order created: ${order.name} (${order.id})`);

        // Activate the order
        if (order.id) {
          await client.orders.activate(order.id);
          console.log(`[Paid.ai] Order activated`);
        }
      } else {
        console.log(`[Paid.ai] Active order found: ${activeOrder.name}`);
      }
    } catch (err: any) {
      console.log(`[Paid.ai] Order setup note: ${err?.message || err}`);
    }

    _setupDone = true;
    return { customerId: customer?.id, productId: product?.id };
  } catch (err) {
    console.error("[Paid.ai] Setup error:", err);
    return {};
  }
}

// ── Signal recording (v2 API — the proper way) ───────────────────

/**
 * Track an API cost event via Paid.ai usageRecordBulkV2.
 * Each call = one signal with full token breakdown + cost data.
 */
export async function trackCost(event: CostEvent) {
  const client = getClient();
  if (!client) {
    console.log(
      `[Paid.ai] (no key) Cost: $${event.estimatedCostUsd.toFixed(4)} for ${event.model}`
    );
    return;
  }

  try {
    await client.usage.usageRecordBulkV2({
      signals: [
        {
          event_name: "llm_api_cost",
          external_product_id: PRODUCT_EXTERNAL_ID,
          external_customer_id: CUSTOMER_EXTERNAL_ID,
          idempotency_key: event.runId || crypto.randomUUID(),
          data: {
            agent_id: event.agentId,
            model: event.model,
            provider: "anthropic",
            input_tokens: event.inputTokens,
            output_tokens: event.outputTokens,
            cache_read_tokens: event.cacheRead || 0,
            cache_write_tokens: event.cacheWrite || 0,
            cost_usd: event.estimatedCostUsd,
            duration_ms: event.durationMs || 0,
            timestamp: event.timestamp,
          },
        },
      ],
    });
    console.log(
      `[Paid.ai] Cost signal sent: $${event.estimatedCostUsd.toFixed(4)} | ${event.model} | ${event.inputTokens}+${event.outputTokens} tokens`
    );
  } catch (err: any) {
    console.error("[Paid.ai] Cost signal error:", err?.message || err);
  }
}

/**
 * Track a revenue event (agent earned money from an app).
 */
export async function trackRevenue(event: RevenueEvent) {
  const client = getClient();
  if (!client) {
    console.log(
      `[Paid.ai] (no key) Revenue: $${event.amountUsd.toFixed(2)} from ${event.source}`
    );
    return;
  }

  try {
    await client.usage.usageRecordBulkV2({
      signals: [
        {
          event_name: "revenue_earned",
          external_product_id: PRODUCT_EXTERNAL_ID,
          external_customer_id: CUSTOMER_EXTERNAL_ID,
          idempotency_key: `rev-${Date.now()}-${crypto.randomUUID().slice(0, 8)}`,
          data: {
            agent_id: event.agentId,
            source: event.source,
            amount_usd: event.amountUsd,
            description: event.description,
            timestamp: event.timestamp,
          },
        },
      ],
    });
    console.log(
      `[Paid.ai] Revenue signal sent: $${event.amountUsd.toFixed(2)} from ${event.source}`
    );
  } catch (err: any) {
    console.error("[Paid.ai] Revenue signal error:", err?.message || err);
  }
}

/**
 * Track a build completion event.
 */
export async function trackBuild(event: BuildEvent) {
  const client = getClient();
  if (!client) {
    console.log(`[Paid.ai] (no key) Build: ${event.appName} (${event.success ? "OK" : "FAIL"})`);
    return;
  }

  try {
    await client.usage.usageRecordBulkV2({
      signals: [
        {
          event_name: "app_build_completed",
          external_product_id: PRODUCT_EXTERNAL_ID,
          external_customer_id: CUSTOMER_EXTERNAL_ID,
          idempotency_key: `build-${event.runId || Date.now()}`,
          data: {
            agent_id: event.agentId,
            app_name: event.appName,
            success: event.success,
            apk_size_bytes: event.apkSize || 0,
          },
        },
      ],
    });
    console.log(
      `[Paid.ai] Build signal sent: ${event.appName} (${event.success ? "SUCCESS" : "FAIL"})`
    );
  } catch (err: any) {
    console.error("[Paid.ai] Build signal error:", err?.message || err);
  }
}

// ── Query cost traces from Paid.ai dashboard ─────────────────────

/**
 * Fetch cost traces from Paid.ai for our customer.
 * This pulls data from Paid.ai's own analytics.
 */
export async function getCostTraces(limit = 50) {
  const client = getClient();
  if (!client) return null;

  try {
    const traces = await client.traces.getTraces({
      externalCustomerId: CUSTOMER_EXTERNAL_ID,
      externalProductId: PRODUCT_EXTERNAL_ID,
      limit,
      offset: 0,
    });
    return traces;
  } catch (err: any) {
    console.error("[Paid.ai] Trace query error:", err?.message || err);
    return null;
  }
}

/**
 * Fetch usage summaries from Paid.ai for our customer.
 */
export async function getUsageSummary() {
  const client = getClient();
  if (!client) return null;

  try {
    const usage = await client.customers.getUsageByExternalId(CUSTOMER_EXTERNAL_ID, {
      limit: 100,
      offset: 0,
    });
    return usage;
  } catch (err: any) {
    console.error("[Paid.ai] Usage query error:", err?.message || err);
    return null;
  }
}

/**
 * Fetch cost data from Paid.ai for our customer.
 */
export async function getCostSummary() {
  const client = getClient();
  if (!client) return null;

  try {
    const costs = await client.customers.getCostsByExternalId(CUSTOMER_EXTERNAL_ID, {
      limit: 100,
      offset: 0,
    });
    return costs;
  } catch (err: any) {
    console.error("[Paid.ai] Cost query error:", err?.message || err);
    return null;
  }
}

/**
 * Check current usage/entitlement status.
 */
export async function checkUsageStatus() {
  const client = getClient();
  if (!client) return null;

  try {
    const status = await client.usage.checkUsage({
      externalCustomerId: CUSTOMER_EXTERNAL_ID,
      externalProductId: PRODUCT_EXTERNAL_ID,
    });
    return status;
  } catch (err: any) {
    console.error("[Paid.ai] Usage check error:", err?.message || err);
    return null;
  }
}

/**
 * List all registered products in Paid.ai.
 */
export async function listProducts() {
  const client = getClient();
  if (!client) return null;

  try {
    return await client.products.list();
  } catch (err: any) {
    console.error("[Paid.ai] List products error:", err?.message || err);
    return null;
  }
}

/**
 * List all customers in Paid.ai.
 */
export async function listCustomers() {
  const client = getClient();
  if (!client) return null;

  try {
    return await client.customers.list();
  } catch (err: any) {
    console.error("[Paid.ai] List customers error:", err?.message || err);
    return null;
  }
}

/**
 * List all orders in Paid.ai.
 */
export async function listOrders() {
  const client = getClient();
  if (!client) return null;

  try {
    return await client.orders.list();
  } catch (err: any) {
    console.error("[Paid.ai] List orders error:", err?.message || err);
    return null;
  }
}
