/**
 * One-time script: import the already-funded wallet into the persistent store.
 * The secret key was generated earlier — we need it to reconstruct the wallet.
 *
 * Since we don't have the secret key of J9CH6BqS... anymore (it wasn't saved),
 * we create a NEW genesis-001 wallet and note it needs funding.
 *
 * Going forward, wallet-store.ts will persist all wallets.
 */
import { getOrCreateWallet } from "./wallet-store.js";
import { getBalance } from "./wallet.js";

const wallet = getOrCreateWallet("genesis-001");
console.log("Genesis wallet:", wallet.publicKey);

const balance = await getBalance(wallet.publicKey);
console.log("Balance:", balance, "SOL");

if (balance === 0) {
  console.log("\n⚠️  This wallet has 0 SOL. Fund it at:");
  console.log(`  https://faucet.solana.com/?address=${wallet.publicKey}`);
}
