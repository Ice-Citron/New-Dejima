const FINANCE_SERVER = "http://localhost:3456";

async function main() {
  try {
    const [ecoRes, evtRes, paidRes] = await Promise.all([
      fetch(`${FINANCE_SERVER}/api/economics`),
      fetch(`${FINANCE_SERVER}/api/events`),
      fetch(`${FINANCE_SERVER}/api/paid/status`),
    ]);
    const economics = (await ecoRes.json()) as Record<string, any>;
    const events = (await evtRes.json()) as any[];
    const paidStatus = (await paidRes.json()) as { configured: boolean };

    console.log("\n\x1b[35m═══════════════════════════════════════════\x1b[0m");
    console.log("  \x1b[1mNEW DEJIMA — SYSTEM STATUS\x1b[0m");
    console.log("\x1b[35m═══════════════════════════════════════════\x1b[0m\n");

    // Paid.ai status
    if (paidStatus.configured) {
      console.log(
        "  \x1b[32m● Paid.ai CONNECTED\x1b[0m — signals streaming to app.paid.ai"
      );
    } else {
      console.log(
        "  \x1b[33m○ Paid.ai NOT CONFIGURED\x1b[0m — set PAID_AI_API_KEY in .env"
      );
    }
    console.log();

    const agents = Object.entries(economics);
    if (agents.length === 0) {
      console.log("  No agent activity recorded yet.\n");
    } else {
      let totalCost = 0,
        totalRevenue = 0;
      for (const [id, d] of agents) {
        const ratio = d.cost > 0 ? d.revenue / d.cost : 0;
        const net = d.revenue - d.cost;
        totalCost += d.cost;
        totalRevenue += d.revenue;

        console.log(`  \x1b[35mAgent:\x1b[0m ${id}`);
        console.log(`    Model:   ${d.lastModel || "unknown"}`);
        console.log(
          `    Cost:    \x1b[31m$${d.cost.toFixed(4)}\x1b[0m  |  Revenue: \x1b[32m$${d.revenue.toFixed(4)}\x1b[0m`
        );
        console.log(
          `    Net:     ${net >= 0 ? "\x1b[32m" : "\x1b[31m"}$${net.toFixed(4)}\x1b[0m`
        );
        console.log(
          `    Tokens:  ${d.tokens.toLocaleString()}  |  Builds: ${d.builds}`
        );
        console.log(
          `    Ratio:   ${ratio >= 1 ? "\x1b[32m" : "\x1b[31m"}${ratio.toFixed(2)}x${ratio >= 1 ? " (SUSTAINABLE)" : " (deficit)"}\x1b[0m`
        );
        console.log();
      }

      const totalNet = totalRevenue - totalCost;
      const totalRatio = totalCost > 0 ? totalRevenue / totalCost : 0;
      console.log("  \x1b[1m── TOTALS ──\x1b[0m");
      console.log(`    Cost:    \x1b[31m$${totalCost.toFixed(4)}\x1b[0m`);
      console.log(`    Revenue: \x1b[32m$${totalRevenue.toFixed(4)}\x1b[0m`);
      console.log(
        `    Net:     ${totalNet >= 0 ? "\x1b[32m" : "\x1b[31m"}$${totalNet.toFixed(4)}\x1b[0m`
      );
      console.log(
        `    Ratio:   ${totalRatio >= 1 ? "\x1b[32m" : "\x1b[31m"}${totalRatio.toFixed(2)}x\x1b[0m`
      );
      console.log();
    }

    console.log(`  Total events: ${events.length}`);
    console.log(`  Dashboard:    http://localhost:3456/`);
    if (paidStatus.configured) {
      console.log(`  Paid.ai:      https://app.paid.ai`);
    }

    // If Paid.ai is connected, also show data from their API
    if (paidStatus.configured) {
      console.log("\n\x1b[35m── Paid.ai Data ──\x1b[0m");
      try {
        const [prodRes, custRes, ordRes] = await Promise.all([
          fetch(`${FINANCE_SERVER}/api/paid/products`),
          fetch(`${FINANCE_SERVER}/api/paid/customers`),
          fetch(`${FINANCE_SERVER}/api/paid/orders`),
        ]);
        const products = await prodRes.json();
        const customers = await custRes.json();
        const orders = await ordRes.json();

        const prodCount = Array.isArray(products) ? products.length : 0;
        const custCount = Array.isArray(customers) ? customers.length : 0;
        const ordCount = Array.isArray(orders) ? orders.length : 0;

        console.log(`    Products:  ${prodCount}`);
        console.log(`    Customers: ${custCount}`);
        console.log(`    Orders:    ${ordCount}`);

        if (Array.isArray(products) && products.length > 0) {
          for (const p of products) {
            console.log(`      \x1b[36m${p.name}\x1b[0m (${p.externalId || p.id})`);
          }
        }
        if (Array.isArray(customers) && customers.length > 0) {
          for (const c of customers) {
            console.log(`      \x1b[36m${c.name}\x1b[0m (${c.externalId || c.id})`);
          }
        }
      } catch {
        console.log("    (could not fetch Paid.ai data)");
      }
    }

    console.log();
  } catch {
    console.error("  Finance server not running. Start with:");
    console.error("    cd finance && npx tsx src/server.ts\n");
  }
}

main();
