import { Connection, Keypair, LAMPORTS_PER_SOL, PublicKey } from "@solana/web3.js";
import bs58 from "bs58";

const DEVNET_URL = "https://api.devnet.solana.com";

// Treasury wallet agent ID — stable across all runs via wallet-store
export const TREASURY_AGENT_ID = "new-dejima-treasury";

export interface AgentWallet {
  publicKey: string;
  secretKeyBase58: string;
  createdAt: string;
  agentId: string;
}

export function createAgentWallet(agentId: string): AgentWallet {
  const keypair = Keypair.generate();
  return {
    publicKey: keypair.publicKey.toBase58(),
    secretKeyBase58: bs58.encode(keypair.secretKey),
    createdAt: new Date().toISOString(),
    agentId,
  };
}

export function loadWallet(secretKeyBase58: string): Keypair {
  return Keypair.fromSecretKey(bs58.decode(secretKeyBase58));
}

export async function getBalance(publicKey: string): Promise<number> {
  const connection = new Connection(DEVNET_URL, "confirmed");
  const balance = await connection.getBalance(new PublicKey(publicKey));
  return balance / LAMPORTS_PER_SOL;
}

export async function airdropDevnetSol(publicKey: string, amount: number = 1): Promise<string> {
  const connection = new Connection(DEVNET_URL, "confirmed");
  const sig = await connection.requestAirdrop(
    new PublicKey(publicKey),
    amount * LAMPORTS_PER_SOL
  );
  const latestBlockhash = await connection.getLatestBlockhash();
  await connection.confirmTransaction({ signature: sig, ...latestBlockhash });
  return sig;
}
