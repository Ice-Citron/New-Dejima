import { useGateway } from "@/hooks/useGateway";
import { TopBar } from "@/components/dashboard/TopBar";
import { SummaryCards } from "@/components/dashboard/SummaryCards";
import { AgentFleet } from "@/components/dashboard/AgentFleet";
import { DailyCostChart } from "@/components/dashboard/DailyCostChart";
import { ProductionLog } from "@/components/dashboard/ProductionLog";
import { CostByAgent } from "@/components/dashboard/CostByAgent";
import { TreasuryWallets } from "@/components/dashboard/TreasuryWallets";
import { DashboardFooter } from "@/components/dashboard/DashboardFooter";

const Index = () => {
  const { status, agents, dailyCosts, totals, totalSessions, isLive, refresh } = useGateway();

  return (
    <div className="relative min-h-screen bg-background bg-grid scanlines">
      {/* Ambient glow orbs */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="absolute -top-40 -left-40 h-80 w-80 rounded-full bg-primary/[0.04] blur-[100px]" />
        <div className="absolute top-1/3 -right-40 h-96 w-96 rounded-full bg-accent/[0.03] blur-[120px]" />
        <div className="absolute bottom-0 left-1/3 h-64 w-64 rounded-full bg-neon-purple/[0.03] blur-[100px]" />
      </div>

      <div className="relative z-10">
        <TopBar status={status} onRefresh={refresh} />

        <main className="mx-auto max-w-[1400px] space-y-10 px-4 py-8 sm:px-6 lg:px-8">
          {/* Hero stats */}
          <SummaryCards agents={agents} totals={totals} totalSessions={totalSessions} isLive={isLive} />

          {/* Agent fleet */}
          <AgentFleet agents={agents} isLive={isLive} />

          {/* Charts side by side */}
          <div className="grid gap-8 lg:grid-cols-2">
            <DailyCostChart dailyCosts={dailyCosts} isLive={isLive} />
            <CostByAgent agents={agents} isLive={isLive} />
          </div>

          {/* Production log */}
          <ProductionLog agents={agents} />

          {/* Treasury */}
          <TreasuryWallets agents={agents} />
        </main>

        <DashboardFooter />
      </div>
    </div>
  );
};

export default Index;
