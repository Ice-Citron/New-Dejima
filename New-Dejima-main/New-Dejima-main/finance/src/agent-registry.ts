import type { AgentWallet } from "./wallet.js";

export interface AgentRecord {
  agentId: string;
  parentId: string | null; // null = genesis agent
  walletAddress: string;
  secretKeyBase58: string;
  model: string;
  status: "alive" | "dead" | "reproducing";
  generation: number; // 0 = genesis, 1 = child, 2 = grandchild...
  createdAt: string;
  totalCostUsd: number;
  totalRevenueUsd: number;
}

// In-memory registry
const registry: Map<string, AgentRecord> = new Map();

export function registerAgent(
  agentId: string,
  wallet: AgentWallet,
  model: string,
  parentId: string | null = null,
  generation: number = 0
): AgentRecord {
  const record: AgentRecord = {
    agentId,
    parentId,
    walletAddress: wallet.publicKey,
    secretKeyBase58: wallet.secretKeyBase58,
    model,
    status: "alive",
    generation,
    createdAt: new Date().toISOString(),
    totalCostUsd: 0,
    totalRevenueUsd: 0,
  };
  registry.set(agentId, record);
  return record;
}

export function getAgent(agentId: string): AgentRecord | undefined {
  return registry.get(agentId);
}

export function getAllAgents(): AgentRecord[] {
  return Array.from(registry.values());
}

export function getChildren(agentId: string): AgentRecord[] {
  return Array.from(registry.values()).filter((a) => a.parentId === agentId);
}

export function updateAgentEconomics(
  agentId: string,
  costUsd: number,
  revenueUsd: number
) {
  const agent = registry.get(agentId);
  if (agent) {
    agent.totalCostUsd += costUsd;
    agent.totalRevenueUsd += revenueUsd;
  }
}

export function formatFamilyTree(rootId: string, indent: number = 0): string {
  const agent = registry.get(rootId);
  if (!agent) return "";
  const prefix = "  ".repeat(indent);
  const ratio =
    agent.totalCostUsd > 0
      ? (agent.totalRevenueUsd / agent.totalCostUsd).toFixed(2) + "x"
      : "N/A";
  let tree = `${prefix}[Gen ${agent.generation}] ${agent.agentId} | ${agent.status.toUpperCase()} | ratio: ${ratio} | wallet: ${agent.walletAddress.slice(0, 8)}...\n`;
  for (const child of getChildren(rootId)) {
    tree += formatFamilyTree(child.agentId, indent + 1);
  }
  return tree;
}
