export function DashboardFooter() {
  return (
    <footer className="border-t border-border/30 py-8">
      <div className="mx-auto max-w-[1400px] px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col items-center gap-4">
          <div className="flex items-center gap-2">
            <span className="text-lg">🏯</span>
            <p className="font-display text-xs tracking-[0.3em] text-muted-foreground/60">
              NEW DEJIMA
            </p>
          </div>
          <p className="text-xs font-mono text-muted-foreground/30">
            Autonomous Agent Economy
          </p>
          <div className="flex items-center gap-6 text-[10px] font-mono text-muted-foreground/40">
            <span className="flex items-center gap-1.5">
              <span className="h-1.5 w-1.5 rounded-full bg-neon-green shadow-[0_0_4px_hsl(160_84%_39%/0.5)]" />
              LIVE = gateway data
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-1.5 w-1.5 rounded-full bg-neon-purple shadow-[0_0_4px_hsl(265_83%_57%/0.5)]" />
              SIM = estimated
            </span>
          </div>
        </div>
      </div>
    </footer>
  );
}
