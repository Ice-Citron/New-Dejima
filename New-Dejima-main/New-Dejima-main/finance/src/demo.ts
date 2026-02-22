/**
 * New Dejima — Track B Demo Script
 *
 * Shows the full agent infrastructure payment pipeline end-to-end:
 *   1. Create/load genesis wallet (Claude Opus persona)
 *   2. Load New Dejima treasury wallet
 *   3. Fund genesis via airdrop
 *   4. Genesis builds Tip Calculator app (economics simulation)
 *   5. Genesis decides to reproduce → sends SOL to treasury
 *      → KYC → SOL/USDT conversion → Stripe → Vast.ai GPU → vLLM → Qwen → handoff
 *   6. Agent writes notes for child
 *   7. Print full economics + family tree
 */
import { airdropDevnetSol, getBalance, TREASURY_AGENT_ID } from "./wallet.js";
import { getOrCreateWallet } from "./wallet-store.js";
import { registerAgent, updateAgentEconomics, formatFamilyTree } from "./agent-registry.js";
import { trackCost, trackRevenue } from "./cost-tracker.js";
import { addEvent } from "./event-store.js";
import { getAgentEconomics, formatEconomicsReport, calculateTokenCost } from "./agent-economics.js";
import { agentReproductionPipeline } from "./payment-pipeline.js";
import { destroyVastInstance, closeSSHTunnel } from "./vast-provision.js";
import { writeNote } from "./agent-notes.js";

const LIVE_MODE = process.argv.includes("--live");
const SOL_PAYMENT = 0.5;

// Track Vast.ai instance for cleanup on exit
let vastInstanceId: number | null = null;

async function cleanup() {
  closeSSHTunnel();
  if (vastInstanceId) {
    console.log(`\n[cleanup] Destroying Vast.ai instance ${vastInstanceId}...`);
    try {
      await destroyVastInstance(vastInstanceId);
    } catch (e: any) {
      console.warn(`[cleanup] Destroy failed: ${e.message}`);
    }
    vastInstanceId = null;
  }
}

process.on("SIGINT", async () => { await cleanup(); process.exit(0); });
process.on("SIGTERM", async () => { await cleanup(); process.exit(0); });

async function main() {
  if (!LIVE_MODE) {
    console.log("  [DRY-RUN] No SOL will be spent. Use --live for real on-chain transfers.\n");
  }

  // ── Step 1: Genesis agent ─────────────────────────────────────────────────
  console.log("Step 1: Creating genesis agent (Claude Opus)...");
  const genesisWallet = getOrCreateWallet("genesis-001");
  registerAgent("genesis-001", genesisWallet, "claude-opus-4-6", null, 0);
  console.log(`  Wallet: ${genesisWallet.publicKey}\n`);

  // ── Step 2: Treasury wallet ───────────────────────────────────────────────
  console.log("Step 2: Loading New Dejima treasury wallet...");
  const treasuryWallet = getOrCreateWallet(TREASURY_AGENT_ID);
  console.log(`  Treasury: ${treasuryWallet.publicKey}\n`);

  // ── Step 3: Fund genesis wallet via airdrop ───────────────────────────────
  console.log("Step 3: Funding genesis wallet via devnet airdrop...");
  try {
    const sig = await airdropDevnetSol(genesisWallet.publicKey, 1);
    const balance = await getBalance(genesisWallet.publicKey);
    console.log(`  Airdrop TX: ${sig}`);
    console.log(`  Balance:    ${balance} SOL\n`);
  } catch {
    const balance = await getBalance(genesisWallet.publicKey).catch(() => 0);
    console.warn("  Airdrop rate-limited or failed.");
    console.warn(`  Fund manually: https://faucet.solana.com/?address=${genesisWallet.publicKey}`);
    console.warn(`  Current balance: ${balance} SOL\n`);
  }

  // ── Step 4: Economics — agent builds an Android app ───────────────────────
  console.log("Step 4: Genesis builds Tip Calculator app...");
  const inputTokens = 80_000;
  const outputTokens = 20_000;
  const costUsd = calculateTokenCost("claude-opus-4-6", inputTokens, outputTokens);
  const revenueUsd = 0.99;
  console.log(`  Tokens:  ${(inputTokens + outputTokens).toLocaleString()}`);
  console.log(`  Cost:    $${costUsd.toFixed(4)}`);
  console.log(`  Revenue: $${revenueUsd}\n`);

  await trackCost({
    agentId: "genesis-001",
    model: "claude-opus-4-6",
    inputTokens,
    outputTokens,
    estimatedCostUsd: costUsd,
    timestamp: new Date().toISOString(),
  });
  addEvent({
    type: "cost",
    agentId: "genesis-001",
    timestamp: new Date().toISOString(),
    data: { model: "claude-opus-4-6", inputTokens, outputTokens, costUsd }
  });
  await trackRevenue({
    agentId: "genesis-001",
    source: "android_app",
    amountUsd: revenueUsd,
    description: "Tip Calculator — 1 download",
    timestamp: new Date().toISOString(),
  });
  addEvent({
    type: "revenue",
    agentId: "genesis-001",
    timestamp: new Date().toISOString(),
    data: { source: "android_app", amountUsd: revenueUsd, description: "Tip Calculator — 1 download" }
  });
  updateAgentEconomics("genesis-001", costUsd, revenueUsd);

  const economics = await getAgentEconomics(
    "genesis-001",
    genesisWallet.publicKey,
    costUsd,
    revenueUsd,
    inputTokens + outputTokens,
    "claude-opus-4-6"
  );
  console.log(formatEconomicsReport(economics));

  // ── Step 5: Full reproduction pipeline ───────────────────────────────────
  console.log("Step 5: Genesis decides to reproduce — running payment pipeline...");

  const genesisBalance = await getBalance(genesisWallet.publicKey).catch(() => 0);

  if (genesisBalance < SOL_PAYMENT) {
    console.log(`  Insufficient balance (${genesisBalance} SOL) to pay treasury.`);
    console.log(`  Fund at: https://faucet.solana.com/?address=${genesisWallet.publicKey}\n`);
    console.log("  Skipping pipeline — add SOL and re-run with --live\n");
  } else if (!LIVE_MODE) {
    console.log("  [DRY-RUN] Has enough SOL but --live not set. Run with --live to spin up real GPU.\n");
  } else {
    const result = await agentReproductionPipeline({
      agentId: "genesis-001",
      agentWallet: genesisWallet,
      treasuryWallet,
      solPayment: SOL_PAYMENT,
      model: "Qwen/Qwen2.5-7B-Instruct",
    });

    // Track Vast instance for cleanup
    if (result.success && result.vast?.instanceId) {
      vastInstanceId = result.vast.instanceId;
    }

    // Register the child agent in the family tree
    if (result.success) {
      const childId = result.handoff.agentId;
      const { createAgentWallet } = await import("./wallet.js");
      const childWallet = createAgentWallet(childId);
      registerAgent(childId, childWallet, "Qwen/Qwen2.5-7B-Instruct", "genesis-001", 1);
      console.log(`  Child agent registered: ${childId} (Gen 1)\n`);
    }
  }

  // ── Step 6: Agent writes notes for child ─────────────────────────────────
  console.log("AGENT NOTE WRITTEN:");
  const note = writeNote(
    "genesis-001",
    "Focus on Android apps first. Tip calculator was $0.99/download. Build 10 apps before spending on reproduction again.",
    true
  );
  console.log(`  "${note.content}"\n`);

  // ── Step 7: Family tree ───────────────────────────────────────────────────
  console.log("Agent Family Tree:");
  console.log(formatFamilyTree("genesis-001"));

  console.log("╔══════════════════════════════════════════════════╗");
  console.log("║              DEMO COMPLETE                        ║");
  console.log("╚══════════════════════════════════════════════════╝\n");

  // Destroy Vast.ai instance now that demo is done
  await cleanup();
}

main().catch(async (err) => {
  console.error("\nDemo error:", err.message);
  await cleanup();
  process.exit(1);
});
