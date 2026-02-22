import { useState, useEffect, useCallback, useRef } from "react";
import {
  Agent, DailyCost, UsageTotals,
  FALLBACK_AGENTS, FALLBACK_DAILY_COSTS, FALLBACK_TOTALS,
} from "@/lib/fallback-data";

type ConnectionStatus = "connecting" | "connected" | "offline";

interface GatewayState {
  status: ConnectionStatus;
  error: string | null;
  agents: Agent[];
  dailyCosts: DailyCost[];
  totals: UsageTotals;
  totalSessions: number;
  isLive: boolean;
}

const GATEWAY_URL = "ws://127.0.0.1:18789";

let idCounter = 0;
const nextId = () => `dejima-${++idCounter}-${Date.now()}`;

export function useGateway() {
  const [state, setState] = useState<GatewayState>({
    status: "connecting",
    error: null,
    agents: FALLBACK_AGENTS,
    dailyCosts: FALLBACK_DAILY_COSTS,
    totals: FALLBACK_TOTALS,
    totalSessions: FALLBACK_AGENTS.reduce((s, a) => s + a.sessions, 0),
    isLive: false,
  });

  const wsRef = useRef<WebSocket | null>(null);
  const pendingRef = useRef<Map<string, (payload: any) => void>>(new Map());

  const goOffline = useCallback(() => {
    setState((s) => ({
      ...s,
      status: "offline",
      isLive: false,
      agents: FALLBACK_AGENTS,
      dailyCosts: FALLBACK_DAILY_COSTS,
      totals: FALLBACK_TOTALS,
      totalSessions: FALLBACK_AGENTS.reduce((sum, a) => sum + a.sessions, 0),
    }));
  }, []);

  const sendReq = useCallback((method: string, params: any): Promise<any> => {
    return new Promise((resolve, reject) => {
      const ws = wsRef.current;
      if (!ws || ws.readyState !== WebSocket.OPEN) {
        reject(new Error("Not connected"));
        return;
      }
      const id = nextId();
      const timeout = setTimeout(() => {
        pendingRef.current.delete(id);
        reject(new Error("Timeout"));
      }, 10000);
      pendingRef.current.set(id, (payload) => {
        clearTimeout(timeout);
        resolve(payload);
      });
      ws.send(JSON.stringify({ type: "req", id, method, params }));
    });
  }, []);

  const fetchAllData = useCallback(async () => {
    try {
      const [agentsRes, usageRes, costRes] = await Promise.all([
        sendReq("agents.list", {}),
        sendReq("sessions.usage", { days: 30, limit: 1000 }),
        sendReq("usage.cost", { days: 30 }),
      ]);

      const agentsList: Agent[] = (agentsRes.agents || []).map((a: any) => {
        const byAgent = usageRes.aggregates?.byAgent?.find((b: any) => b.agentId === a.id);
        return {
          id: a.id,
          name: a.identity?.name || a.name || a.id,
          emoji: a.identity?.emoji || "🤖",
          role: a.identity?.theme || "Agent",
          model: a.model || "unknown",
          status: "active" as const,
          color: "#6366f1",
          tokens: byAgent?.totals?.totalTokens || 0,
          cost: byAgent?.totals?.totalCost || 0,
          revenue: 0,
          sessions: 0,
          productions: [],
        };
      });

      setState((s) => ({
        ...s,
        status: "connected",
        isLive: true,
        agents: agentsList.length > 0 ? agentsList : FALLBACK_AGENTS,
        dailyCosts: costRes.daily || FALLBACK_DAILY_COSTS,
        totals: costRes.totals || usageRes.totals || FALLBACK_TOTALS,
        totalSessions: usageRes.sessions?.length || 0,
      }));
    } catch {
      goOffline();
    }
  }, [sendReq, goOffline]);

  const connect = useCallback(() => {
    setState((s) => ({ ...s, status: "connecting", error: null }));
    try {
      const ws = new WebSocket(GATEWAY_URL);
      wsRef.current = ws;
      let handshakeDone = false;

      const connectTimeout = setTimeout(() => {
        if (!handshakeDone) {
          ws.close();
          goOffline();
        }
      }, 10000);

      ws.onopen = () => {
        // Wait 800ms for challenge, then send connect anyway
        setTimeout(() => {
          if (!handshakeDone) {
            const id = nextId();
            pendingRef.current.set(id, () => {
              handshakeDone = true;
              clearTimeout(connectTimeout);
              fetchAllData();
            });
            ws.send(JSON.stringify({
              type: "req", id, method: "connect",
              params: {
                minProtocol: 1, maxProtocol: 3,
                client: { id: "dejima-dashboard", displayName: "New Dejima Dashboard", version: "1.0.0", platform: "web", mode: "control" },
                caps: [],
              },
            }));
          }
        }, 800);
      };

      ws.onmessage = (ev) => {
        try {
          const msg = JSON.parse(ev.data);
          if (msg.type === "event" && msg.event === "connect.challenge") {
            // Send connect immediately on challenge
            const id = nextId();
            pendingRef.current.set(id, () => {
              handshakeDone = true;
              clearTimeout(connectTimeout);
              fetchAllData();
            });
            ws.send(JSON.stringify({
              type: "req", id, method: "connect",
              params: {
                minProtocol: 1, maxProtocol: 3,
                client: { id: "dejima-dashboard", displayName: "New Dejima Dashboard", version: "1.0.0", platform: "web", mode: "control" },
                caps: [],
              },
            }));
          } else if (msg.type === "res" && msg.id) {
            const handler = pendingRef.current.get(msg.id);
            if (handler) {
              pendingRef.current.delete(msg.id);
              handler(msg.payload);
            }
          }
        } catch { /* ignore parse errors */ }
      };

      ws.onerror = () => {
        clearTimeout(connectTimeout);
        goOffline();
      };

      ws.onclose = () => {
        clearTimeout(connectTimeout);
        if (!handshakeDone) goOffline();
      };
    } catch {
      goOffline();
    }
  }, [goOffline, fetchAllData]);

  useEffect(() => {
    connect();
    return () => { wsRef.current?.close(); };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const refresh = useCallback(() => {
    wsRef.current?.close();
    connect();
  }, [connect]);

  return { ...state, refresh };
}
