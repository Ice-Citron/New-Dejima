import type { Agent } from "@/lib/fallback-data";
import { Wallet } from "lucide-react";

interface TreasuryWalletsProps {
  agents: Agent[];
}

export function TreasuryWallets({ agents }: TreasuryWalletsProps) {
  const activeAgents = agents.filter((a) => a.status === "active");

  return (
    <section className="animate-fade-up" style={{ animationDelay: "0.6s" }}>
      <div className="mb-5 flex items-center gap-3">
        <div className="h-px flex-1 bg-gradient-to-r from-warning/30 to-transparent" />
        <h2 className="font-display text-xs font-bold tracking-[0.25em] text-warning">
          TREASURY & WALLETS
        </h2>
        <div className="h-px flex-1 bg-gradient-to-l from-warning/30 to-transparent" />
      </div>

      <div className="glass-card rounded-xl border-dashed p-6">
        {/* Header */}
        <div className="mb-5 flex items-center gap-3">
          <span className="inline-flex items-center gap-1.5 rounded-md border border-warning/20 bg-warning/10 px-2.5 py-1 text-[10px] font-display tracking-wider text-warning">
            ⏳ PENDING INTEGRATION
          </span>
        </div>

        {/* Description */}
        <div className="mb-6 flex items-start gap-3 rounded-lg bg-secondary/30 p-4 border border-border/30">
          <span className="text-2xl mt-0.5">💰</span>
          <p className="text-sm text-muted-foreground leading-relaxed">
            Crypto wallet integration is under development. Once ready, each agent will have its own
            wallet address for <span className="text-foreground font-medium">autonomous revenue collection</span> and{" "}
            <span className="text-foreground font-medium">cost settlement</span>.
          </p>
        </div>

        {/* Wallet grid */}
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {activeAgents.map((agent) => (
            <div
              key={agent.id}
              className="rounded-lg border border-dashed border-border/40 bg-secondary/20 px-4 py-3 transition-all hover:border-primary/20 hover:bg-secondary/30"
            >
              <div className="flex items-center gap-2">
                <span className="text-lg">{agent.emoji}</span>
                <span className="text-sm font-medium text-foreground">{agent.name}</span>
              </div>
              <div className="mt-2 flex items-center gap-1.5">
                <Wallet className="h-3 w-3 text-muted-foreground/30" />
                <span className="font-mono text-[11px] text-muted-foreground/40">
                  0x???...??? (pending)
                </span>
              </div>
              <p className="mt-0.5 font-mono text-xs text-muted-foreground/25 ml-[18px]">-- ETH</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
