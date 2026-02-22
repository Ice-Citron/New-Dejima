import { StatusBadge } from "./LiveBadge";
import { formatTokens, formatCost } from "@/lib/format";
import type { Agent } from "@/lib/fallback-data";
import { CreditCard, Cpu } from "lucide-react";

interface AgentFleetProps {
  agents: Agent[];
  isLive: boolean;
}

export function AgentFleet({ agents, isLive }: AgentFleetProps) {
  return (
    <section>
      <div className="mb-5 flex items-center gap-3">
        <div className="h-px flex-1 bg-gradient-to-r from-primary/40 to-transparent" />
        <h2 className="font-display text-xs font-bold tracking-[0.25em] text-primary text-glow-cyan">
          AGENT FLEET
        </h2>
        <div className="h-px flex-1 bg-gradient-to-l from-primary/40 to-transparent" />
      </div>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {agents.map((agent, i) => (
          <AgentCard key={agent.id} agent={agent} isLive={isLive} index={i} />
        ))}
      </div>
    </section>
  );
}

function AgentCard({ agent, isLive, index }: { agent: Agent; isLive: boolean; index: number }) {
  const isPlanned = agent.status === "planned";
  const maxBar = Math.max(agent.cost, agent.revenue, 1);

  return (
    <div
      className="glass-card glass-card-hover gradient-border rounded-xl p-5 animate-fade-up"
      style={{ animationDelay: `${index * 80}ms` }}
    >
      {/* Header */}
      <div className="flex items-start gap-3">
        <div
          className="relative flex h-12 w-12 items-center justify-center rounded-xl text-xl"
          style={{
            backgroundColor: agent.color + "15",
            boxShadow: `0 0 20px ${agent.color}20, inset 0 0 20px ${agent.color}10`,
          }}
        >
          {agent.emoji}
          {!isPlanned && (
            <span
              className="absolute -right-0.5 -top-0.5 h-2.5 w-2.5 rounded-full border-2 border-background pulse-dot"
              style={{ backgroundColor: agent.color }}
            />
          )}
        </div>
        <div className="flex-1 min-w-0">
          <h3 className="font-semibold text-foreground truncate">{agent.name}</h3>
          <p className="text-[11px] text-muted-foreground truncate">{agent.role}</p>
          {!isPlanned && (
            <div className="mt-1 flex items-center gap-1 text-[10px] font-mono text-muted-foreground/60">
              <Cpu className="h-2.5 w-2.5" />
              <span className="truncate">{agent.model}</span>
            </div>
          )}
        </div>
      </div>

      {isPlanned ? (
        <div className="mt-6 flex flex-col items-center gap-2 py-4">
          <div className="h-8 w-8 rounded-full border border-dashed border-muted-foreground/20 flex items-center justify-center">
            <span className="text-muted-foreground/30 text-xs">⏳</span>
          </div>
          <p className="text-xs font-mono text-muted-foreground/40 tracking-wide">
            NOT YET OPERATIONAL
          </p>
        </div>
      ) : (
        <>
          {/* Metrics */}
          <div className="mt-4 grid grid-cols-2 gap-2">
            <MetricCell label="Tokens" value={formatTokens(agent.tokens)} isLive={isLive} />
            <MetricCell label="Cost" value={formatCost(agent.cost)} isLive={isLive} />
            <MetricCell label="Revenue" value={formatCost(agent.revenue)} isLive={false} />
            <MetricCell label="Sessions" value={String(agent.sessions)} isLive={isLive} />
          </div>

          {/* ROI bar */}
          {(agent.cost > 0 || agent.revenue > 0) && (
            <div className="mt-3">
              <div className="flex justify-between text-[9px] font-mono text-muted-foreground/50 mb-1">
                <span>COST</span>
                <span>REVENUE</span>
              </div>
              <div className="flex h-1.5 overflow-hidden rounded-full bg-secondary">
                <div
                  className="h-full rounded-full bar-shimmer"
                  style={{
                    width: `${(agent.cost / maxBar) * 100}%`,
                    backgroundColor: agent.color,
                    boxShadow: `0 0 8px ${agent.color}40`,
                  }}
                />
                <div
                  className="h-full bg-neon-green rounded-full"
                  style={{ width: `${(agent.revenue / maxBar) * 100}%` }}
                />
              </div>
            </div>
          )}
        </>
      )}

      {/* Wallet */}
      <div className="mt-4 flex items-center gap-1.5 rounded-md border border-dashed border-border/50 bg-secondary/30 px-3 py-2 text-[10px] font-mono text-muted-foreground/40">
        <CreditCard className="h-3 w-3" />
        0x???...??? (pending)
      </div>
    </div>
  );
}

function MetricCell({ label, value, isLive }: { label: string; value: string; isLive: boolean }) {
  return (
    <div className="rounded-lg bg-secondary/40 px-3 py-2.5 border border-border/30">
      <div className="flex items-center justify-between">
        <p className="text-[9px] font-display tracking-wider text-muted-foreground/60">{label.toUpperCase()}</p>
        <StatusBadge isLive={isLive} />
      </div>
      <p className="mt-1 font-mono text-sm font-bold text-foreground ticker">{value}</p>
    </div>
  );
}
