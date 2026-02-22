import { getBalance } from "./wallet.js";
import { getOrCreateWallet } from "./wallet-store.js";
import { registerAgent, formatFamilyTree, getAllAgents } from "./agent-registry.js";
import { reproduce } from "./reproduce.js";

async function main() {
  console.log("=== Agent Reproduction Test ===\n");

  // 1. Load (or create) persistent genesis wallet
  console.log("Loading genesis agent wallet...");
  const genesisWallet = getOrCreateWallet("genesis-001");
  registerAgent("genesis-001", genesisWallet, "claude-opus-4-6", null, 0);
  console.log("Genesis wallet:", genesisWallet.publicKey);

  const balance = await getBalance(genesisWallet.publicKey);
  if (balance < 0.1) {
    console.warn("Insufficient balance:", balance, "SOL — fund at:");
    console.warn(`  https://faucet.solana.com/?address=${genesisWallet.publicKey}`);
    return;
  }
  console.log(`Genesis balance: ${balance} SOL`);

  // 3. Genesis reproduces → Gen 1 child
  console.log("\n--- Genesis reproducing ---");
  const result1 = await reproduce("genesis-001");
  if (!result1.success) {
    console.error("Reproduction failed:", result1.error);
    return;
  }

  // 4. Child reproduces → Gen 2 grandchild
  console.log("--- Child reproducing ---");
  const result2 = await reproduce(result1.childId!);
  if (result2.success) {
    console.log("Grandchild created:", result2.childId);
  }

  // 5. Print balances
  console.log("\n=== Final Balances ===");
  for (const agent of getAllAgents()) {
    const bal = await getBalance(agent.walletAddress);
    console.log(`  ${agent.agentId} | Gen ${agent.generation} | ${bal.toFixed(4)} SOL`);
  }

  // 6. Print family tree
  console.log("\n=== Agent Family Tree ===");
  console.log(formatFamilyTree("genesis-001"));
}

main().catch(console.error);
