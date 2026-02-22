import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Area, AreaChart,
} from "recharts";
import { StatusBadge } from "./LiveBadge";
import { formatDate } from "@/lib/format";
import type { DailyCost } from "@/lib/fallback-data";

interface DailyCostChartProps {
  dailyCosts: DailyCost[];
  isLive: boolean;
}

const CustomTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="glass-card rounded-lg border border-primary/20 px-3 py-2 shadow-xl">
      <p className="text-[10px] font-mono text-muted-foreground">{formatDate(label)}</p>
      <p className="font-mono text-sm font-bold text-primary">
        ${payload[0].value.toFixed(2)}
      </p>
      {payload[0]?.payload?.totalTokens && (
        <p className="text-[10px] font-mono text-muted-foreground/60">
          {payload[0].payload.totalTokens.toLocaleString()} tokens
        </p>
      )}
    </div>
  );
};

export function DailyCostChart({ dailyCosts, isLive }: DailyCostChartProps) {
  const last14 = dailyCosts.slice(-14);

  return (
    <section className="animate-fade-up" style={{ animationDelay: "0.3s" }}>
      <div className="mb-4 flex items-center gap-3">
        <h2 className="font-display text-xs font-bold tracking-[0.2em] text-foreground">
          DAILY COST
        </h2>
        <StatusBadge isLive={isLive} />
        <span className="text-[10px] font-mono text-muted-foreground/40">14D WINDOW</span>
      </div>
      {last14.length === 0 ? (
        <div className="glass-card gradient-border rounded-xl p-10 text-center">
          <p className="font-mono text-sm text-muted-foreground/50">
            Connect gateway for cost history
          </p>
        </div>
      ) : (
        <div className="glass-card gradient-border rounded-xl p-4 pt-6">
          <ResponsiveContainer width="100%" height={240}>
            <AreaChart data={last14}>
              <defs>
                <linearGradient id="costGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="hsl(187, 94%, 43%)" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="hsl(187, 94%, 43%)" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid
                strokeDasharray="3 3"
                stroke="hsl(222, 20%, 14%)"
                vertical={false}
              />
              <XAxis
                dataKey="date"
                tickFormatter={formatDate}
                tick={{ fill: "hsl(215, 20%, 40%)", fontSize: 10, fontFamily: "JetBrains Mono" }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                tickFormatter={(v) => `$${v}`}
                tick={{ fill: "hsl(215, 20%, 40%)", fontSize: 10, fontFamily: "JetBrains Mono" }}
                axisLine={false}
                tickLine={false}
                width={45}
              />
              <Tooltip content={<CustomTooltip />} />
              <Area
                type="monotone"
                dataKey="totalCost"
                stroke="hsl(187, 94%, 43%)"
                strokeWidth={2}
                fill="url(#costGradient)"
                dot={{ fill: "hsl(187, 94%, 43%)", r: 3, strokeWidth: 0 }}
                activeDot={{
                  fill: "hsl(187, 94%, 43%)",
                  r: 5,
                  strokeWidth: 2,
                  stroke: "hsl(222, 47%, 4%)",
                }}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      )}
    </section>
  );
}
