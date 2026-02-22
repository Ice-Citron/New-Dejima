/**
 * Persistent wallet store — saves/loads agent wallets to a JSON file
 * so the same address is reused across runs.
 */
import { readFileSync, writeFileSync, existsSync } from "fs";
import type { AgentWallet } from "./wallet.js";
import { createAgentWallet } from "./wallet.js";

const STORE_PATH = new URL("../../wallets.json", import.meta.url).pathname;

type WalletStore = Record<string, AgentWallet>;

function loadStore(): WalletStore {
  if (!existsSync(STORE_PATH)) return {};
  return JSON.parse(readFileSync(STORE_PATH, "utf-8"));
}

function saveStore(store: WalletStore) {
  writeFileSync(STORE_PATH, JSON.stringify(store, null, 2));
}

/** Get existing wallet or create a new one — always same address for same agentId */
export function getOrCreateWallet(agentId: string): AgentWallet {
  const store = loadStore();
  if (store[agentId]) {
    console.log(`  [wallet-store] Loaded existing wallet for ${agentId}`);
    return store[agentId];
  }
  const wallet = createAgentWallet(agentId);
  store[agentId] = wallet;
  saveStore(store);
  console.log(`  [wallet-store] Created new wallet for ${agentId}: ${wallet.publicKey}`);
  return wallet;
}

export function injectFundedWallet(agentId: string, wallet: AgentWallet) {
  const store = loadStore();
  store[agentId] = wallet;
  saveStore(store);
}
