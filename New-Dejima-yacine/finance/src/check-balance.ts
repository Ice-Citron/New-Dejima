import { getBalance } from "./wallet.js";

const addr = "J9CH6BqSTjEkZbp7s5gtRhEnthMNVJm2F6gRWbekENF";
const balance = await getBalance(addr);
console.log(`Balance of ${addr.slice(0, 8)}...: ${balance} SOL`);
