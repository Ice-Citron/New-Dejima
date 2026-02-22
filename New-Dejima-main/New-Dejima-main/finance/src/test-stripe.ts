import { createAgentCard, getAgentSpend } from "./stripe-agent.js";

async function main() {
  console.log("=== Stripe Virtual Card Test ===\n");

  console.log("Creating virtual card for agent-001...");
  const card = await createAgentCard("agent-001", 50000); // $500/day limit
  console.log("Card ID:   ", card.cardId);
  console.log("Last 4:    ", card.last4);
  console.log("Cardholder:", card.cardholderId);

  const spend = await getAgentSpend(card.cardId);
  console.log("Total spend: $" + spend);
}

main().catch(console.error);
