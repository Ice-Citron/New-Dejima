/**
 * Agent reproduction — new model.
 *
 * Instead of splitting SOL 50/50 with a child wallet, the agent pays
 * New Dejima's treasury in SOL. New Dejima then runs the full payment
 * pipeline: KYC → conversion → Stripe → Crusoe provisioning → server
 * handoff. The "child" is the new agent that gets a running server.
 */
import { getBalance } from "./wallet.js";
import { getOrCreateWallet } from "./wallet-store.js";
import { getAgent, registerAgent } from "./agent-registry.js";
import { agentReproductionPipeline, type PipelineResult } from "./payment-pipeline.js";
import { TREASURY_AGENT_ID } from "./wallet.js";
import { trackCost } from "./cost-tracker.js";

// Minimum SOL the parent must hold before reproduction is allowed
const MIN_BALANCE_TO_REPRODUCE = 0.1;

export interface ReproductionResult {
  success: boolean;
  childId?: string;
  childWallet?: string;
  seedCapitalSol?: number;
  transferTx?: string;
  pipeline?: PipelineResult;
  error?: string;
}

export async function canReproduce(
  parentId: string
): Promise<{ allowed: boolean; reason: string }> {
  const parent = getAgent(parentId);
  if (!parent) return { allowed: false, reason: "Parent agent not found" };
  if (parent.status !== "alive")
    return { allowed: false, reason: "Parent agent is not alive" };

  const balance = await getBalance(parent.walletAddress);
  if (balance < MIN_BALANCE_TO_REPRODUCE) {
    return {
      allowed: false,
      reason: `Insufficient balance: ${balance.toFixed(4)} SOL (need ${MIN_BALANCE_TO_REPRODUCE} SOL)`,
    };
  }

  return {
    allowed: true,
    reason: `Balance OK: ${balance.toFixed(4)} SOL`,
  };
}

/**
 * Reproduce: parent agent pays New Dejima treasury → full pipeline runs →
 * child agent gets a provisioned Crusoe server.
 *
 * Keeps the same exported signature so existing callers don't break.
 */
export async function reproduce(
  parentId: string,
  solPayment: number = 0.5,
  model: "qwen-14b" | "qwen-70b" = "qwen-14b"
): Promise<ReproductionResult> {
  const check = await canReproduce(parentId);
  if (!check.allowed) {
    return { success: false, error: check.reason };
  }

  const parent = getAgent(parentId)!;
  const parentWallet = {
    publicKey: parent.walletAddress,
    secretKeyBase58: parent.secretKeyBase58,
    createdAt: parent.createdAt,
    agentId: parent.agentId,
  };

  // Ensure treasury wallet exists
  const treasuryWallet = getOrCreateWallet(TREASURY_AGENT_ID);

  const pipeline = await agentReproductionPipeline({
    agentId: parentId,
    agentWallet: parentWallet,
    treasuryWallet,
    solPayment,
    model,
  });

  if (!pipeline.success) {
    return { success: false, error: pipeline.error ?? "Pipeline failed", pipeline };
  }

  // Register the child agent (uses treasury-provisioned server)
  const childId = `${parentId}-child-${Date.now().toString(36)}`;
  // Child gets a fresh wallet (no SOL needed — it has a server instead)
  const { createAgentWallet } = await import("./wallet.js");
  const childWallet = createAgentWallet(childId);

  registerAgent(childId, childWallet, model, parentId, parent.generation + 1);

  // Paid.ai — track reproduction as a cost event for the parent agent
  await trackCost({
    agentId: parentId,
    model: "reproduction",
    inputTokens: 0,
    outputTokens: 0,
    estimatedCostUsd: solPayment * 150, // SOL → USD at ~$150/SOL
    timestamp: new Date().toISOString(),
  });

  return {
    success: true,
    childId,
    childWallet: childWallet.publicKey,
    seedCapitalSol: solPayment,
    transferTx: pipeline.payment.txSig,
    pipeline,
  };
}
