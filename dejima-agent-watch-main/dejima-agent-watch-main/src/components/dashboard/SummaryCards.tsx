import { Bot, Coins, DollarSign, TrendingUp, Package, Activity } from "lucide-react";
import { StatusBadge } from "./LiveBadge";
import { formatTokens, formatCost } from "@/lib/format";
import type { Agent, UsageTotals } from "@/lib/fallback-data";

interface SummaryCardsProps {
  agents: Agent[];
  totals: UsageTotals;
  totalSessions: number;
  isLive: boolean;
}

export function SummaryCards({ agents, totals, totalSessions, isLive }: SummaryCardsProps) {
  const activeCount = agents.filter((a) => a.status === "active").length;
  const plannedCount = agents.filter((a) => a.status === "planned").length;
  const productCount = agents.reduce((s, a) => s + a.productions.length, 0);
  const verifiedCount = agents.reduce(
    (s, a) => s + a.productions.filter((p) => p.status === "verified").length, 0
  );

  const cards = [
    {
      label: "ACTIVE AGENTS", value: String(activeCount), sub: `${plannedCount} planned`,
      icon: <Bot className="h-4 w-4" />, color: "text-primary", glow: "shadow-[0_0_20px_hsl(187_94%_43%/0.1)]",
      borderColor: "hover:border-primary/40", showBadge: false,
    },
    {
      label: "TOKENS (30D)", value: formatTokens(totals.totalTokens), sub: "all agents",
      icon: <Coins className="h-4 w-4" />, color: "text-neon-amber", glow: "shadow-[0_0_20px_hsl(45_93%_58%/0.1)]",
      borderColor: "hover:border-neon-amber/40", showBadge: true,
    },
    {
      label: "COST (30D)", value: formatCost(totals.totalCost), sub: "API spend",
      icon: <DollarSign className="h-4 w-4" />, color: "text-accent", glow: "shadow-[0_0_20px_hsl(330_81%_60%/0.1)]",
      borderColor: "hover:border-accent/40", showBadge: true,
    },
    {
      label: "REVENUE", value: "$0.00", sub: "wallet pending",
      icon: <TrendingUp className="h-4 w-4" />, color: "text-neon-green", glow: "shadow-[0_0_20px_hsl(160_84%_39%/0.1)]",
      borderColor: "hover:border-neon-green/40", showBadge: false,
    },
    {
      label: "SHIPPED", value: String(productCount), sub: `${verifiedCount} verified`,
      icon: <Package className="h-4 w-4" />, color: "text-neon-orange", glow: "shadow-[0_0_20px_hsl(25_95%_53%/0.1)]",
      borderColor: "hover:border-neon-orange/40", showBadge: false,
    },
    {
      label: "SESSIONS", value: String(totalSessions), sub: "30 day window",
      icon: <Activity className="h-4 w-4" />, color: "text-neon-purple", glow: "shadow-[0_0_20px_hsl(265_83%_57%/0.1)]",
      borderColor: "hover:border-neon-purple/40", showBadge: true,
    },
  ];

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-6">
      {cards.map((card, i) => (
        <div
          key={card.label}
          className={`glass-card glass-card-hover rounded-xl p-4 animate-fade-up ${card.borderColor} ${card.glow}`}
          style={{ animationDelay: `${i * 60}ms` }}
        >
          <div className="flex items-center justify-between">
            <span className={card.color}>{card.icon}</span>
            {card.showBadge && <StatusBadge isLive={isLive} />}
          </div>
          <p className={`mt-3 font-mono text-2xl font-bold ticker ${card.color}`}>
            {card.value}
          </p>
          <p className="mt-1 text-[10px] font-display tracking-wider text-muted-foreground">
            {card.label}
          </p>
          <p className="text-[10px] font-mono text-muted-foreground/50">{card.sub}</p>
        </div>
      ))}
    </div>
  );
}
