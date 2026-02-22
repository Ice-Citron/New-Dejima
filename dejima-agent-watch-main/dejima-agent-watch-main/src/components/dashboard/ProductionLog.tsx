import type { Agent } from "@/lib/fallback-data";

interface ProductionLogProps {
  agents: Agent[];
}

export function ProductionLog({ agents }: ProductionLogProps) {
  const rows = agents.flatMap((a) =>
    a.productions.map((p) => ({ ...p, agentName: a.name, agentEmoji: a.emoji, agentColor: a.color }))
  );

  const statusStyles: Record<string, { dot: string; text: string }> = {
    verified: { dot: "bg-neon-green shadow-[0_0_6px_hsl(160_84%_39%/0.5)]", text: "text-neon-green" },
    compiled: { dot: "bg-info shadow-[0_0_6px_hsl(199_89%_48%/0.5)]", text: "text-info" },
    demo: { dot: "bg-neon-purple shadow-[0_0_6px_hsl(265_83%_57%/0.5)]", text: "text-neon-purple" },
  };

  return (
    <section className="animate-fade-up" style={{ animationDelay: "0.5s" }}>
      <div className="mb-4 flex items-center gap-3">
        <div className="h-px flex-1 bg-gradient-to-r from-accent/40 to-transparent" />
        <h2 className="font-display text-xs font-bold tracking-[0.25em] text-accent text-glow-magenta">
          PRODUCTION LOG
        </h2>
        <div className="h-px flex-1 bg-gradient-to-l from-accent/40 to-transparent" />
      </div>
      {rows.length === 0 ? (
        <div className="glass-card gradient-border rounded-xl p-8 text-center">
          <p className="font-mono text-sm text-muted-foreground/40">No productions logged</p>
        </div>
      ) : (
        <div className="glass-card gradient-border overflow-hidden rounded-xl">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border/50">
                {["PRODUCT", "AGENT", "TYPE", "STATUS", "SIZE", "DATE"].map((h, i) => (
                  <th
                    key={h}
                    className={`px-5 py-3 text-left text-[10px] font-display tracking-[0.15em] text-muted-foreground/60 ${
                      i >= 2 && i <= 4 ? "hidden sm:table-cell" : ""
                    } ${i === 5 ? "hidden sm:table-cell" : ""}`}
                  >
                    {h}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {rows.map((row, i) => {
                const s = statusStyles[row.status] || statusStyles.demo;
                return (
                  <tr
                    key={i}
                    className="border-b border-border/20 last:border-0 transition-colors hover:bg-primary/[0.03]"
                  >
                    <td className="px-5 py-3.5 font-medium text-foreground">{row.name}</td>
                    <td className="px-5 py-3.5 text-muted-foreground">
                      <span className="mr-1.5">{row.agentEmoji}</span>
                      {row.agentName}
                    </td>
                    <td className="hidden px-5 py-3.5 font-mono text-xs text-muted-foreground/60 sm:table-cell">
                      {row.type}
                    </td>
                    <td className="px-5 py-3.5">
                      <span className={`inline-flex items-center gap-1.5 text-xs font-mono ${s.text}`}>
                        <span className={`h-1.5 w-1.5 rounded-full ${s.dot}`} />
                        {row.status.toUpperCase()}
                      </span>
                    </td>
                    <td className="hidden px-5 py-3.5 font-mono text-xs text-muted-foreground/50 sm:table-cell">
                      {row.size}
                    </td>
                    <td className="hidden px-5 py-3.5 font-mono text-xs text-muted-foreground/50 sm:table-cell">
                      {row.date}
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
