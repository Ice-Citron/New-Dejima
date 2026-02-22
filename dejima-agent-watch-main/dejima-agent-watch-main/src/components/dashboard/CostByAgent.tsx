import { StatusBadge } from "./LiveBadge";
import { formatCost } from "@/lib/format";
import type { Agent } from "@/lib/fallback-data";

interface CostByAgentProps {
  agents: Agent[];
  isLive: boolean;
}

export function CostByAgent({ agents, isLive }: CostByAgentProps) {
  const withCost = agents.filter((a) => a.cost > 0).sort((a, b) => b.cost - a.cost);
  const maxCost = withCost[0]?.cost || 1;

  return (
    <section className="animate-fade-up" style={{ animationDelay: "0.35s" }}>
      <div className="mb-4 flex items-center gap-3">
        <h2 className="font-display text-xs font-bold tracking-[0.2em] text-foreground">
          COST BY AGENT
        </h2>
        <StatusBadge isLive={isLive} />
      </div>
      {withCost.length === 0 ? (
        <div className="glass-card gradient-border rounded-xl p-10 text-center">
          <p className="font-mono text-sm text-muted-foreground/50">No cost data</p>
        </div>
      ) : (
        <div className="glass-card gradient-border rounded-xl p-5 space-y-4">
          {withCost.map((agent, i) => {
            const pct = (agent.cost / maxCost) * 100;
            return (
              <div
                key={agent.id}
                className="group animate-fade-up"
                style={{ animationDelay: `${0.4 + i * 0.08}s` }}
              >
                <div className="flex items-center gap-3 mb-1.5">
                  <div
                    className="flex h-7 w-7 items-center justify-center rounded-lg text-sm"
                    style={{ backgroundColor: agent.color + "18" }}
                  >
                    {agent.emoji}
                  </div>
                  <span className="flex-1 text-sm font-medium text-foreground">{agent.name}</span>
                  <span className="font-mono text-sm font-bold text-foreground">
                    {formatCost(agent.cost)}
                  </span>
                </div>
                <div className="ml-10 h-2 overflow-hidden rounded-full bg-secondary/60">
                  <div
                    className="h-full rounded-full bar-shimmer transition-all duration-700"
                    style={{
                      width: `${pct}%`,
                      background: `linear-gradient(90deg, ${agent.color}CC, ${agent.color})`,
                      boxShadow: `0 0 12px ${agent.color}40`,
                    }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
