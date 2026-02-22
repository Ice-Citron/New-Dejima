import { RefreshCw, Wifi, WifiOff, Loader2, Radio } from "lucide-react";

interface TopBarProps {
  status: "connecting" | "connected" | "offline";
  onRefresh: () => void;
}

export function TopBar({ status, onRefresh }: TopBarProps) {
  return (
    <header className="sticky top-0 z-50 border-b border-border/50 bg-background/60 backdrop-blur-2xl">
      <div className="mx-auto flex max-w-[1400px] items-center justify-between px-4 py-3 sm:px-6 lg:px-8">
        {/* Left — brand */}
        <div className="flex items-center gap-3">
          <div className="relative flex h-9 w-9 items-center justify-center">
            <span className="text-2xl animate-float">🏯</span>
            <div className="absolute inset-0 rounded-lg bg-primary/10 blur-md" />
          </div>
          <div>
            <h1 className="font-display text-sm font-bold tracking-[0.2em] text-primary text-glow-cyan">
              NEW DEJIMA
            </h1>
            <p className="text-[10px] font-mono tracking-wider text-muted-foreground">
              AGENT ECONOMICS v1.0
            </p>
          </div>
        </div>

        {/* Right — status + actions */}
        <div className="flex items-center gap-2">
          {/* Connection */}
          <div
            className={`flex items-center gap-1.5 rounded-lg border px-3 py-1.5 text-[11px] font-mono font-medium transition-all ${
              status === "connected"
                ? "border-neon-green/30 bg-neon-green/5 text-neon-green"
                : status === "connecting"
                ? "border-warning/30 bg-warning/5 text-warning"
                : "border-destructive/30 bg-destructive/5 text-destructive"
            }`}
          >
            {status === "connected" && <><Radio className="h-3 w-3" /><span className="hidden sm:inline">GATEWAY LINKED</span></>}
            {status === "connecting" && <><Loader2 className="h-3 w-3 animate-spin" /><span className="hidden sm:inline">CONNECTING</span></>}
            {status === "offline" && <><WifiOff className="h-3 w-3" /><span className="hidden sm:inline">OFFLINE</span></>}
          </div>

          {/* Wallet */}
          <div className="hidden items-center gap-1.5 rounded-lg border border-warning/20 bg-warning/5 px-3 py-1.5 text-[11px] font-mono font-medium text-warning sm:flex">
            <span>💳</span> WALLET PENDING
          </div>

          {/* Refresh */}
          <button
            onClick={onRefresh}
            className="group flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-secondary text-muted-foreground transition-all hover:border-primary/40 hover:text-primary hover:shadow-[0_0_15px_hsl(187_94%_43%/0.15)]"
          >
            <RefreshCw className="h-3.5 w-3.5 transition-transform group-hover:rotate-90" />
          </button>
        </div>
      </div>
    </header>
  );
}
