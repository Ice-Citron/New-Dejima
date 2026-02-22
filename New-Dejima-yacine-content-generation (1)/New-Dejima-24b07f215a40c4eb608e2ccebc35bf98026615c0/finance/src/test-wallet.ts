import { createAgentWallet, airdropDevnetSol, getBalance } from "./wallet.js";

async function main() {
  console.log("=== Wallet Creation ===");
  const wallet = createAgentWallet("agent-001");
  console.log("Wallet created:", wallet.publicKey);
  console.log("Agent ID:      ", wallet.agentId);
  console.log("Created at:    ", wallet.createdAt);

  console.log("\n=== Airdrop ===");
  try {
    const sig = await airdropDevnetSol(wallet.publicKey, 1);
    console.log("Airdrop TX:", sig);
    const balance = await getBalance(wallet.publicKey);
    console.log("Balance:   ", balance, "SOL");
  } catch (err) {
    console.warn("Airdrop failed (rate limited). Get SOL manually:");
    console.warn(`  https://faucet.solana.com/?address=${wallet.publicKey}`);
    console.log("\nChecking balance anyway...");
    const balance = await getBalance(wallet.publicKey);
    console.log("Balance:", balance, "SOL");
  }
}

main().catch(console.error);
