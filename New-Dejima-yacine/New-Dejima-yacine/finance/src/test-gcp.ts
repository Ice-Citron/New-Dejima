/**
 * Standalone GCP GPU test — spin up a real L4/T4 VM, run nvidia-smi, destroy.
 * No SOL, no Stripe, no full pipeline. Just the GPU part.
 */
import { provisionGcpInstance, destroyGcpInstance, closeGcpSSHTunnel } from "./gcp-provision.js";
import dotenv from "dotenv";
dotenv.config();

let instanceName: string | null = null;
let instanceZone: string | null = null;

async function cleanup() {
  closeGcpSSHTunnel();
  if (instanceName && instanceZone) {
    console.log(`\n[cleanup] Destroying ${instanceName} in ${instanceZone}...`);
    try {
      await destroyGcpInstance(instanceName, instanceZone);
      console.log("[cleanup] Done.");
    } catch (e: any) {
      console.warn("[cleanup] Destroy failed:", e.message);
    }
  }
  process.exit(0);
}

process.on("SIGINT",  cleanup);
process.on("SIGTERM", cleanup);

async function main() {
  console.log("═══════════════════════════════════════════");
  console.log("  NEW DEJIMA — GCP GPU STANDALONE TEST");
  console.log("═══════════════════════════════════════════\n");
  console.log("Spinning up real GPU VM on GCP...");
  console.log("(T4 zones will be tried first, falls back to L4)\n");

  const instance = await provisionGcpInstance(
    "Qwen/Qwen2.5-7B-Instruct-AWQ",
    8001
  );

  instanceName = instance.instanceName;
  instanceZone = instance.zone;

  console.log("\n═══════════════════════════════════════════");
  console.log("  RESULT");
  console.log("═══════════════════════════════════════════");
  console.log(`  Instance: ${instance.instanceName}`);
  console.log(`  Zone:     ${instance.zone}`);
  console.log(`  GPU:      ${instance.gpuType}`);
  console.log(`  IP:       ${instance.externalIp}`);
  console.log(`  Price:    $${instance.pricePerHour}/hr`);
  console.log(`  Status:   ${instance.status}`);
  console.log(`  vLLM:     ${instance.status === "ready" ? "✅ loaded" : "⏳ still loading"}`);
  console.log("\n  nvidia-smi output:");
  console.log(instance.nvidiaSmiOutput);
  console.log("═══════════════════════════════════════════\n");

  await cleanup();
}

main().catch(async (err) => {
  console.error("\nError:", err.message);
  await cleanup();
});
