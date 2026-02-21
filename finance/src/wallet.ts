import { Connection, Keypair, LAMPORTS_PER_SOL, PublicKey } from "@solana/web3.js";
import bs58 from "bs58";

const DEVNET_URL = "https://api.devnet.solana.com";
const DEVNET_RPC_FALLBACKS = [
  "https://api.devnet.solana.com",
  "https://devnet.helius-rpc.com/?api-key=demo",
];

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
  for (const rpc of DEVNET_RPC_FALLBACKS) {
    try {
      const connection = new Connection(rpc, "confirmed");
      const sig = await connection.requestAirdrop(
        new PublicKey(publicKey),
        amount * LAMPORTS_PER_SOL
      );
      const latestBlockhash = await connection.getLatestBlockhash();
      await connection.confirmTransaction({ signature: sig, ...latestBlockhash });
      return sig;
    } catch (err) {
      console.warn(`Airdrop via ${rpc} failed, trying next...`);
    }
  }
  throw new Error("All airdrop RPC endpoints failed — try https://faucet.solana.com manually");
}
