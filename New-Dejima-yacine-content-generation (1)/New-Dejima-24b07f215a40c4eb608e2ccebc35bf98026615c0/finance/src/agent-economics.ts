import { getBalance } from "./wallet.js";

// Model cost per million tokens (USD)
const MODEL_COSTS: Record<string, { input: number; output: number }> = {
  "claude-opus-4-6": { input: 15, output: 75 },
  "claude-sonnet-4-6": { input: 3, output: 15 },
  "gemini-3-pro": { input: 1.25, output: 5 },
  "gemini-3-flash": { input: 0.1, output: 0.4 },
  "meta-llama/Llama-3.3-70B-Instruct": { input: 0.25, output: 0.75 },
  "deepseek-r1": { input: 1.35, output: 5.4 },
};

export interface AgentEconomics {
  agentId: string;
  walletAddress: string;
  walletBalanceSol: number;
  totalCostUsd: number;
  totalRevenueUsd: number;
  netProfitUsd: number;
  moneyMadePerToken: number;
  costPerToken: number;
  sustainabilityRatio: number; // > 1.0 = self-sustaining
  model: string;
}

export function calculateTokenCost(
  model: string,
  inputTokens: number,
  outputTokens: number
): number {
  const costs = MODEL_COSTS[model] ?? { input: 10, output: 50 };
  return (inputTokens * costs.input + outputTokens * costs.output) / 1_000_000;
}

export async function getAgentEconomics(
  agentId: string,
  walletAddress: string,
  totalCostUsd: number,
  totalRevenueUsd: number,
  totalTokens: number,
  model: string
): Promise<AgentEconomics> {
  const walletBalance = await getBalance(walletAddress);

  const moneyMadePerToken = totalTokens > 0 ? totalRevenueUsd / totalTokens : 0;
  const costPerToken = totalTokens > 0 ? totalCostUsd / totalTokens : 0;
  const sustainabilityRatio = costPerToken > 0 ? moneyMadePerToken / costPerToken : 0;

  return {
    agentId,
    walletAddress,
    walletBalanceSol: walletBalance,
    totalCostUsd,
    totalRevenueUsd,
    netProfitUsd: totalRevenueUsd - totalCostUsd,
    moneyMadePerToken,
    costPerToken,
    sustainabilityRatio,
    model,
  };
}

export function formatEconomicsReport(e: AgentEconomics): string {
  const status =
    e.sustainabilityRatio >= 1.0
      ? "SELF-SUSTAINING ✓"
      : `DEFICIT — need ${(1 / Math.max(e.sustainabilityRatio, 0.0001)).toFixed(1)}x improvement`;

  return `
═══════════════════════════════════════════
  AGENT ECONOMICS: ${e.agentId}
═══════════════════════════════════════════
  Model:              ${e.model}
  Wallet:             ${e.walletAddress.slice(0, 8)}...
  Wallet Balance:     ${e.walletBalanceSol.toFixed(4)} SOL

  Total Cost:         $${e.totalCostUsd.toFixed(4)}
  Total Revenue:      $${e.totalRevenueUsd.toFixed(4)}
  Net Profit:         $${e.netProfitUsd.toFixed(4)}

  Cost/Token:         $${e.costPerToken.toFixed(8)}
  Revenue/Token:      $${e.moneyMadePerToken.toFixed(8)}

  SUSTAINABILITY:     ${e.sustainabilityRatio.toFixed(2)}x
  STATUS: ${status}
═══════════════════════════════════════════
`;
}
