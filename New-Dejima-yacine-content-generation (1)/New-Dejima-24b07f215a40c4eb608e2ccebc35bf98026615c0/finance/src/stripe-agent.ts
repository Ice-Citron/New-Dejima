import Stripe from "stripe";
import dotenv from "dotenv";
dotenv.config();

const stripe = new Stripe(process.env.STRIPE_SECRET_KEY!);

// Create a virtual card for an agent with spending controls
export async function createAgentCard(agentId: string, dailyLimitCents: number = 50000) {
  // Step 1: Create cardholder
  // Sanitize agentId for Stripe (letters only for name field)
  const safeName = agentId.replace(/[^a-zA-Z ]/g, " ").replace(/\s+/g, " ").trim() || "Dejima Agent";
  const cardholder = await stripe.issuing.cardholders.create({
    name: safeName,
    email: `${agentId.replace(/[^a-z0-9]/gi, "")}@dejima.ai`,
    phone_number: "+15551234567",
    status: "active",
    type: "individual",
    individual: {
      first_name: "Dejima",
      last_name: "Agent",
      dob: { day: 1, month: 1, year: 1990 },
    },
    billing: {
      address: {
        line1: "1 Rue de Rivoli",
        city: "Paris",
        postal_code: "75001",
        country: "FR",
      },
    },
  });

  // Step 2: Create virtual card with spending limits
  const card = await stripe.issuing.cards.create({
    cardholder: cardholder.id,
    currency: "eur",
    type: "virtual",
    status: "active",
    spending_controls: {
      spending_limits: [
        {
          amount: dailyLimitCents,
          interval: "daily",
        },
      ],
    },
  });

  return {
    cardId: card.id,
    cardholderId: cardholder.id,
    agentId,
    last4: card.last4,
  };
}

// List all transactions for an agent's card
export async function getAgentTransactions(cardId: string) {
  const transactions = await stripe.issuing.transactions.list({
    card: cardId,
    limit: 50,
  });
  return transactions.data;
}

// Get total spend for an agent
export async function getAgentSpend(cardId: string): Promise<number> {
  const transactions = await getAgentTransactions(cardId);
  return transactions.reduce((sum, tx) => sum + tx.amount, 0) / 100;
}
