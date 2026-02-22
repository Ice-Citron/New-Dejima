import { createAgentWallet } from "./wallet.js";
import { getAgentEconomics, formatEconomicsReport, calculateTokenCost } from "./agent-economics.js";

async function main() {
  console.log("=== Agent Economics Test ===\n");

  const wallet = createAgentWallet("agent-001");
  console.log("Wallet:", wallet.publicKey);

  // Calculate real cost for 100k tokens on Claude Opus
  const cost = calculateTokenCost("claude-opus-4-6", 80_000, 20_000);
  console.log(`Cost for 80k input + 20k output tokens: $${cost.toFixed(4)}`);

  // Simulate: agent used 100k tokens, cost $2.70, earned $0.99
  const economics = await getAgentEconomics(
    "agent-001",
    wallet.publicKey,
    2.70,    // total cost
    0.99,    // total revenue (1 app download)
    100_000, // total tokens
    "claude-opus-4-6"
  );

  console.log(formatEconomicsReport(economics));
}

main().catch(console.error);
