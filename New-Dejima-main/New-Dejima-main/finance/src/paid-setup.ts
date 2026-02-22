/**
 * New Dejima — Paid.ai Setup & Demo Script
 *
 * Run this to:
 *   1. Initialize Paid.ai customer + product + order
 *   2. Send sample signals for all 19 built apps
 *   3. Verify data appears in Paid.ai dashboard (app.paid.ai)
 *
 * Usage: npx tsx src/paid-setup.ts
 */
import { createRequire } from "module";
const require = createRequire(import.meta.url);
const { PaidClient } = require("@paid-ai/paid-node");
import dotenv from "dotenv";
import crypto from "crypto";
dotenv.config();

const PAID_API_KEY = process.env.PAID_AI_API_KEY ?? "";

if (!PAID_API_KEY || PAID_API_KEY === "your_paid_ai_key_here") {
  console.error("\n  ERROR: Set PAID_AI_API_KEY in finance/.env");
  console.error("  Get your key at: https://app.paid.ai\n");
  process.exit(1);
}

const client = new PaidClient({ token: PAID_API_KEY });

// All 19 apps built by the agent
const APPS = [
  "tip-calculator",
  "workout-timer",
  "mood-tracker",
  "unit-converter",
  "flashcard-app",
  "tic-tac-toe",
  "habit-tracker",
  "expense-tracker",
  "weather-dashboard",
  "crypto-tracker",
  "recipe-finder",
  "trivia-game",
  "space-explorer",
  "news-aggregator",
  "pomodoro-timer",
  "bmi-calculator",
  "color-palette",
  "quote-generator",
  "todo-list",
];

async function main() {
  console.log("\n  ═══════════════════════════════════════════");
  console.log("  PAID.AI SETUP — NEW DEJIMA");
  console.log("  ═══════════════════════════════════════════\n");

  // ── 1. Create/find customer ──
  console.log("  [1/5] Setting up customer...");
  let customer: any;
  try {
    customer = await client.customers.getByExternalId("new-dejima-system");
    console.log(`    Found: ${customer.name} (${customer.id})`);
  } catch {
    customer = await client.customers.create({
      name: "New Dejima AI System",
      externalId: "new-dejima-system",
    });
    console.log(`    Created: ${customer.name} (${customer.id})`);
  }

  // ── 2. Create/find product (agent compute) ──
  console.log("  [2/5] Setting up product...");
  let product: any;
  try {
    product = await client.products.getByExternalId("dejima-agent-compute");
    console.log(`    Found: ${product.name} (${product.id})`);
  } catch {
    product = await client.products.create({
      name: "Dejima Agent Compute",
      description:
        "AI agent inference costs — Claude Opus 4.6 + Gemini 3.1 Pro. Tracks LLM API calls, token usage, and build completions.",
      externalId: "dejima-agent-compute",
      type: "agent",
    });
    console.log(`    Created: ${product.name} (${product.id})`);
  }

  // ── 3. Create/find revenue product ──
  console.log("  [3/5] Setting up revenue product...");
  let revenueProduct: any;
  try {
    revenueProduct = await client.products.getByExternalId("dejima-app-revenue");
    console.log(`    Found: ${revenueProduct.name} (${revenueProduct.id})`);
  } catch {
    revenueProduct = await client.products.create({
      name: "Dejima App Revenue",
      description:
        "Revenue from autonomously-built Android apps — subscriptions, ads, and in-app purchases. The MONEY-MADE side of the money-made > cost equation.",
      externalId: "dejima-app-revenue",
      type: "product",
    });
    console.log(`    Created: ${revenueProduct.name} (${revenueProduct.id})`);
  }

  // ── 4. Create order ──
  console.log("  [4/5] Setting up order...");
  try {
    const orders = await client.orders.list();
    const existing = (orders || []).find(
      (o: any) => o.customerId === customer.id || o.customer?.externalId === "new-dejima-system"
    );
    if (existing) {
      console.log(`    Found: ${existing.name} (${existing.id})`);
    } else {
      const now = new Date();
      const end = new Date(now);
      end.setMonth(end.getMonth() + 1);
      const order = await client.orders.create({
        customerExternalId: "new-dejima-system",
        name: `HackEurope Paris — ${now.toISOString().slice(0, 10)}`,
        description:
          "Hackathon billing period. Tracks all AI compute costs vs app revenue to prove sustainability.",
        startDate: now.toISOString().slice(0, 10),
        endDate: end.toISOString().slice(0, 10),
        currency: "USD",
      });
      console.log(`    Created: ${order.name} (${order.id})`);
      if (order.id) {
        await client.orders.activate(order.id);
        console.log("    Activated order");
      }
    }
  } catch (err: any) {
    console.log(`    Order note: ${err?.message || err}`);
  }

  // ── 5. Send demo signals for all 19 apps ──
  console.log("  [5/5] Sending signals for all 19 apps...\n");

  const signals: any[] = [];

  for (const app of APPS) {
    // Cost signal (LLM inference)
    const inputTokens = 5000 + Math.floor(Math.random() * 20000);
    const outputTokens = 2000 + Math.floor(Math.random() * 15000);
    const costUsd = (inputTokens * 15 + outputTokens * 75) / 1_000_000;

    signals.push({
      event_name: "llm_api_cost",
      external_product_id: "dejima-agent-compute",
      external_customer_id: "new-dejima-system",
      idempotency_key: `demo-cost-${app}-${Date.now()}`,
      data: {
        agent_id: "c3po-dev",
        app_name: app,
        model: "claude-opus-4-6",
        provider: "anthropic",
        input_tokens: inputTokens,
        output_tokens: outputTokens,
        cost_usd: costUsd,
      },
    });

    // Build signal
    signals.push({
      event_name: "app_build_completed",
      external_product_id: "dejima-agent-compute",
      external_customer_id: "new-dejima-system",
      idempotency_key: `demo-build-${app}-${Date.now()}`,
      data: {
        agent_id: "c3po-dev",
        app_name: app,
        success: true,
        first_attempt: true,
      },
    });

    // Revenue signal (simulated subscription)
    const monthlyRevenue = 4.99 + Math.random() * 5;
    signals.push({
      event_name: "revenue_earned",
      external_product_id: "dejima-app-revenue",
      external_customer_id: "new-dejima-system",
      idempotency_key: `demo-rev-${app}-${Date.now()}`,
      data: {
        agent_id: "c3po-dev",
        app_name: app,
        source: "android_subscription",
        amount_usd: parseFloat(monthlyRevenue.toFixed(2)),
        description: `${app} monthly subscription`,
      },
    });
  }

  // Send all signals in bulk
  try {
    await client.usage.usageRecordBulkV2({ signals });
    console.log(`    Sent ${signals.length} signals (${APPS.length} apps x 3 signal types)`);
  } catch (err: any) {
    console.error(`    Signal error: ${err?.message || err}`);
  }

  // ── Summary ──
  console.log("\n  ═══════════════════════════════════════════");
  console.log("  SETUP COMPLETE");
  console.log("  ═══════════════════════════════════════════");
  console.log(`  Customer:  ${customer.name} (${customer.externalId})`);
  console.log(`  Product:   ${product.name} (${product.externalId})`);
  console.log(`  Revenue:   ${revenueProduct.name} (${revenueProduct.externalId})`);
  console.log(`  Signals:   ${signals.length} sent`);
  console.log(`  Dashboard: https://app.paid.ai`);
  console.log("  ═══════════════════════════════════════════\n");

  // ── Verify ──
  console.log("  Verifying...");
  try {
    const products = await client.products.list();
    console.log(`    Products in Paid.ai: ${(products || []).length}`);
    for (const p of products || []) {
      console.log(`      - ${p.name} (${p.externalId || p.id}) [${p.type}]`);
    }
  } catch {}

  try {
    const customers = await client.customers.list();
    console.log(`    Customers in Paid.ai: ${(customers || []).length}`);
    for (const c of customers || []) {
      console.log(`      - ${c.name} (${c.externalId || c.id})`);
    }
  } catch {}

  console.log("\n  Done! Check https://app.paid.ai for your data.\n");
}

main().catch(console.error);
