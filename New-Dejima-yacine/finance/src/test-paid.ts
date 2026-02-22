import { trackCost, trackRevenue } from "./cost-tracker.js";

async function main() {
  console.log("=== Paid.ai Cost Tracking Test ===\n");

  // Simulate an API cost event (agent called Claude Opus)
  await trackCost({
    agentId: "agent-001",
    model: "claude-opus-4-6",
    inputTokens: 5000,
    outputTokens: 2000,
    estimatedCostUsd: 0.225, // $15/M input + $75/M output
    timestamp: new Date().toISOString(),
  });

  // Simulate a revenue event (app downloaded)
  await trackRevenue({
    agentId: "agent-001",
    source: "android_app",
    amountUsd: 0.99,
    description: "Tip Calculator app download",
    timestamp: new Date().toISOString(),
  });

  console.log("\nDone. Check your Paid.ai dashboard at https://app.paid.ai");
}

main().catch(console.error);
