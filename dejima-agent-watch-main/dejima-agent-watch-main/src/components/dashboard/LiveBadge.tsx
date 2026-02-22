interface StatusBadgeProps {
  isLive: boolean;
  className?: string;
}

export function StatusBadge({ isLive, className = "" }: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-md px-1.5 py-0.5 text-[9px] font-bold uppercase tracking-widest ${
        isLive
          ? "bg-neon-green/10 text-neon-green border border-neon-green/20"
          : "bg-neon-purple/10 text-neon-purple border border-neon-purple/20"
      } ${className}`}
    >
      <span className={`h-1 w-1 rounded-full ${isLive ? "bg-neon-green pulse-dot" : "bg-neon-purple"}`} />
      {isLive ? "LIVE" : "SIM"}
    </span>
  );
}
