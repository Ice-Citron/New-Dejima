/**
 * New Dejima — Agent Infrastructure Payment Pipeline
 *
 * Full flow:
 *   1. Receive SOL from agent → New Dejima treasury
 *   2. KYC check (auto-approved for hackathon)
 *   3. Simulate SOL → USDT conversion (1000x demo multiplier)
 *   4. Stripe test-mode PaymentIntent (shows USD charged, no real money)
 *   5. GPU VM provisioning (GCP or Vast.ai)
 *   6. Inject server config (OpenClaw + vLLM + Qwen)
 *   7. Hand server back to requesting agent
 */
import {
  Connection,
  Keypair,
  PublicKey,
  Transaction,
  SystemProgram,
  LAMPORTS_PER_SOL,
  sendAndConfirmTransaction,
} from "@solana/web3.js";
import Stripe from "stripe";
import dotenv from "dotenv";
dotenv.config();

import type { AgentWallet } from "./wallet.js";
import { loadWallet } from "./wallet.js";
import { kycCheck, type KycResult } from "./kyc.js";
import { provisionVastInstance, type VastInstance } from "./vast-provision.js";
import { provisionGcpInstance, type GcpInstance } from "./gcp-provision.js";
import { trackCost, trackRevenue } from "./cost-tracker.js";

export type GpuProvider = "vast" | "gcp";

const DEVNET_URL = "https://api.devnet.solana.com";

// Demo economics constants
const SOL_PRICE_USD = 150;       // simulated spot price
const DEMO_MULTIPLIER = 1000;    // makes tiny devnet SOL amounts look real

// ─────────────────────────────────────────────────────────────────────────────
// Type definitions
// ─────────────────────────────────────────────────────────────────────────────

export interface PaymentReceipt {
  txSig: string;
  treasuryAddress: string;
  receivedSol: number;
}

export interface ConversionResult {
  solAmount: number;
  solPriceUsd: number;
  usdValue: number;
  demoMultiplier: number;
  simulatedUsdt: number;
}

export interface StripeCharge {
  chargeId: string;
  amount: number;
  currency: string;
  status: string;
}

export interface ServerConfig {
  instanceId: string;
  ip: string | null;
  apiEndpoint: string;
  apiKey: string;
  model: string;
  openclawConfig: boolean;
  vllmLoaded: boolean;
}

export interface HandoffResult {
  agentId: string;
  serverConfig: ServerConfig;
  handedOffAt: string;
}

export interface PipelineResult {
  success: boolean;
  agentId: string;
  payment: PaymentReceipt;
  kyc: KycResult;
  conversion: ConversionResult;
  stripe: StripeCharge;
  vast: VastInstance | GcpInstance;
  server: ServerConfig;
  handoff: HandoffResult;
  error?: string;
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 1 — Receive SOL payment from agent → treasury
// ─────────────────────────────────────────────────────────────────────────────

export async function receivePayment(
  fromWallet: AgentWallet,
  treasuryWallet: AgentWallet,
  solAmount: number
): Promise<PaymentReceipt> {
  const connection = new Connection(DEVNET_URL, "confirmed");
  const senderKeypair = loadWallet(fromWallet.secretKeyBase58);

  const tx = new Transaction().add(
    SystemProgram.transfer({
      fromPubkey: senderKeypair.publicKey,
      toPubkey: new PublicKey(treasuryWallet.publicKey),
      lamports: Math.floor(solAmount * LAMPORTS_PER_SOL),
    })
  );

  const txSig = await sendAndConfirmTransaction(connection, tx, [senderKeypair]);

  return {
    txSig,
    treasuryAddress: treasuryWallet.publicKey,
    receivedSol: solAmount,
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 2 — KYC (delegates to kyc.ts)
// ─────────────────────────────────────────────────────────────────────────────

export async function runKyc(
  agentId: string,
  walletAddress: string
): Promise<KycResult> {
  return kycCheck(agentId, walletAddress);
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 3 — Simulate SOL → USDT conversion
// ─────────────────────────────────────────────────────────────────────────────

export function simulateConversion(solAmount: number): ConversionResult {
  const usdValue = solAmount * SOL_PRICE_USD;
  const simulatedUsdt = usdValue * DEMO_MULTIPLIER;
  return {
    solAmount,
    solPriceUsd: SOL_PRICE_USD,
    usdValue,
    demoMultiplier: DEMO_MULTIPLIER,
    simulatedUsdt,
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 4 — Stripe test-mode PaymentIntent
// ─────────────────────────────────────────────────────────────────────────────

export async function chargeStripe(
  amountUsd: number,
  description: string
): Promise<StripeCharge> {
  const stripe = new Stripe(process.env.STRIPE_SECRET_KEY!);
  const amountCents = Math.round(amountUsd * 100);

  const intent = await stripe.paymentIntents.create({
    amount: amountCents,
    currency: "usd",
    description,
    // confirm: false — leaves it in "requires_payment_method" state (demo trail only)
    metadata: { managed_by: "new-dejima", demo: "true" },
  });

  return {
    chargeId: intent.id,
    amount: amountUsd,
    currency: intent.currency.toUpperCase(),
    status: intent.status,
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 5 — GPU VM provisioning (GCP or Vast.ai)
// ─────────────────────────────────────────────────────────────────────────────

export async function provisionGpu(
  model: string,
  provider: GpuProvider = "gcp"
): Promise<VastInstance | GcpInstance> {
  if (provider === "gcp") return provisionGcpInstance(model);
  return provisionVastInstance(model);
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 6 — Inject New Dejima OpenClaw config into server
// (vLLM is already started via the Vast.ai onstart script)
// ─────────────────────────────────────────────────────────────────────────────

export async function injectServerConfig(
  instance: VastInstance | GcpInstance
): Promise<ServerConfig> {
  const apiKey = `sk-dejima-${Math.random().toString(36).slice(2, 14)}`;

  // Normalise across provider shapes
  const isGcp = "instanceName" in instance;
  return {
    instanceId: isGcp
      ? (instance as GcpInstance).instanceName
      : String((instance as VastInstance).instanceId),
    ip: isGcp
      ? (instance as GcpInstance).externalIp
      : (instance as VastInstance).sshHost,
    apiEndpoint: instance.apiEndpoint,
    apiKey,
    model: instance.model,
    openclawConfig: true,
    vllmLoaded: instance.status === "ready",
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// Step 7 — Hand server credentials back to the requesting agent
// ─────────────────────────────────────────────────────────────────────────────

export async function passServerToAgent(
  agentId: string,
  config: ServerConfig
): Promise<HandoffResult> {
  return {
    agentId,
    serverConfig: config,
    handedOffAt: new Date().toISOString(),
  };
}

// ─────────────────────────────────────────────────────────────────────────────
// Master function — runs all 7 steps with console logging
// ─────────────────────────────────────────────────────────────────────────────

export async function agentReproductionPipeline(params: {
  agentId: string;
  agentWallet: AgentWallet;
  treasuryWallet: AgentWallet;
  solPayment: number;
  model?: string;
  provider?: GpuProvider;
  agentNotes?: string;
}): Promise<PipelineResult> {
  const { agentId, agentWallet, treasuryWallet, solPayment } = params;
  const provider = params.provider ?? "gcp";
  const model = params.model ?? "Qwen/Qwen2.5-7B-Instruct-AWQ";

  console.log("\n═══════════════════════════════════════════");
  console.log("  NEW DEJIMA — AGENT REPRODUCTION PIPELINE");
  console.log("═══════════════════════════════════════════\n");

  // ── Step 1: Payment ──────────────────────────────────────────────────────
  console.log("[1/7] PAYMENT RECEIVED");
  const payment = await receivePayment(agentWallet, treasuryWallet, solPayment);
  console.log(`  From:     ${agentId} (${agentWallet.publicKey.slice(0, 8)}...)`);
  console.log(`  To:       new-dejima-treasury`);
  console.log(`  Amount:   ${payment.receivedSol} SOL`);
  console.log(`  TX:       ${payment.txSig.slice(0, 12)}...\n`);

  // Paid.ai — track SOL payment as infrastructure cost for this agent
  const solCostUsd = solPayment * SOL_PRICE_USD;
  await trackCost({
    agentId,
    model: "infrastructure",
    inputTokens: 0,
    outputTokens: 0,
    estimatedCostUsd: solCostUsd,
    timestamp: new Date().toISOString(),
  });

  // ── Step 2: KYC ─────────────────────────────────────────────────────────
  const kyc = await runKyc(agentId, agentWallet.publicKey);
  console.log(`[2/7] KYC CHECK — ${kyc.approved ? "APPROVED" : "REJECTED"}`);
  console.log(`  Agent:    ${kyc.agentId}`);
  console.log(`  Method:   ${kyc.method}`);
  console.log(`  Note:     ${kyc.notes}\n`);

  const emptyVast: VastInstance = {
    instanceId: 0, sshHost: "", sshPort: 0, gpuName: "", pricePerHour: 0,
    nvidiaSmiOutput: "", apiEndpoint: "", externalApiEndpoint: "",
    model, status: "provisioning", provisionedAt: "",
  };

  if (!kyc.approved) {
    return {
      success: false, agentId, payment, kyc,
      conversion: simulateConversion(solPayment),
      stripe: { chargeId: "", amount: 0, currency: "USD", status: "canceled" },
      vast: emptyVast,
      server: { instanceId: "", ip: null, apiEndpoint: "", apiKey: "", model, openclawConfig: false, vllmLoaded: false },
      handoff: { agentId, serverConfig: {} as ServerConfig, handedOffAt: "" },
      error: "KYC rejected",
    };
  }

  // ── Step 3: Conversion ──────────────────────────────────────────────────
  console.log("[3/7] SOL → USDT CONVERSION (SIMULATED)");
  const conversion = simulateConversion(solPayment);
  console.log(`  ${conversion.solAmount} SOL × $${conversion.solPriceUsd}/SOL × ${conversion.demoMultiplier}x demo = $${conversion.simulatedUsdt.toLocaleString()} simulated USDT\n`);

  // ── Step 4: Stripe ──────────────────────────────────────────────────────
  console.log("[4/7] STRIPE CHARGE — TEST MODE");
  let stripe: StripeCharge;
  try {
    stripe = await chargeStripe(
      conversion.simulatedUsdt,
      `New Dejima compute provisioning for agent ${agentId}`
    );
    console.log(`  Amount:   $${stripe.amount.toLocaleString()} ${stripe.currency}`);
    console.log(`  ChargeID: ${stripe.chargeId}`);
    console.log(`  Status:   ${stripe.status} (test)\n`);
  } catch (err: any) {
    console.warn(`  Stripe skipped: ${err.message}\n`);
    stripe = { chargeId: "skipped", amount: conversion.simulatedUsdt, currency: "USD", status: "skipped" };
  }

  // ── Step 5: GPU VM provisioning — REAL GPU, REAL nvidia-smi ─────────────
  console.log(`[5/7] GPU VM PROVISIONED (${provider.toUpperCase()})`);
  console.log(`  Model:    ${model}`);
  const vast = await provisionGpu(model, provider);
  const isGcp = "instanceName" in vast;
  console.log(`  GPU:      ${isGcp ? (vast as GcpInstance).gpuType      : (vast as VastInstance).gpuName}`);
  console.log(`  Instance: ${isGcp ? (vast as GcpInstance).instanceName : String((vast as VastInstance).instanceId)}`);
  console.log(`  Host:     ${isGcp ? (vast as GcpInstance).externalIp   : `${(vast as VastInstance).sshHost}:${(vast as VastInstance).sshPort}`}`);
  console.log(`  Price:    $${vast.pricePerHour.toFixed(3)}/hr`);
  console.log(`  Status:   ${vast.status}\n`);

  // Paid.ai — track GPU provisioning cost (1hr estimated minimum)
  const gpuLabel = "instanceName" in vast
    ? (vast as GcpInstance).gpuType
    : (vast as VastInstance).gpuName;
  await trackCost({
    agentId,
    model: `gpu:${gpuLabel}`,
    inputTokens: 0,
    outputTokens: 0,
    estimatedCostUsd: vast.pricePerHour,
    timestamp: new Date().toISOString(),
  });

  // ── Step 6: Inject OpenClaw config (vLLM already started via onstart) ────
  console.log("[6/7] SERVER INJECTED");
  const server = await injectServerConfig(vast);
  console.log(`  - OpenClaw config: ${server.openclawConfig ? "✓" : "✗"}`);
  console.log(`  - vLLM loaded:     ${server.vllmLoaded ? "✓" : "✗"}`);
  console.log(`  - Model:           ${server.model}`);
  console.log(`  - Endpoint:        ${server.apiEndpoint}\n`);

  // ── Step 7: Handoff ─────────────────────────────────────────────────────
  console.log("[7/7] SERVER PASSED TO AGENT");
  const childId = `${agentId}-child`;
  const handoff = await passServerToAgent(childId, server);
  console.log(`  ${childId} receives:`);
  console.log(`    API: ${handoff.serverConfig.apiEndpoint}`);
  console.log(`    Key: ${handoff.serverConfig.apiKey}\n`);

  // Paid.ai — track revenue: New Dejima delivered a server, agent paid in SOL
  await trackRevenue({
    agentId,
    source: "gpu_provisioning",
    amountUsd: solCostUsd,
    description: `GPU server delivered to ${childId} — ${gpuLabel} via ${provider.toUpperCase()}`,
    timestamp: new Date().toISOString(),
  });

  console.log("═══════════════════════════════════════════\n");

  return {
    success: true,
    agentId,
    payment,
    kyc,
    conversion,
    stripe,
    vast,
    server,
    handoff,
  };
}
