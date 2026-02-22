/**
 * Agent Session Reader
 *
 * Reads OpenClaw agent session data from disk (JSONL transcripts).
 * Provides endpoints for:
 *   - Listing all agents with status
 *   - Getting an agent's current task / status
 *   - Reading prompt/response message history
 *   - Streaming live activity via SSE
 */
import { readFileSync, existsSync, statSync, readdirSync, watchFile, unwatchFile } from "fs";
import { join } from "path";
import type { Response } from "express";

// OpenClaw stores sessions here
const SESSIONS_DIR =
  process.env.OPENCLAW_SESSIONS_DIR ||
  join(process.env.HOME || "/Users/administrator", ".openclaw-dev", "agents", "dev", "sessions");

const SESSIONS_INDEX = join(SESSIONS_DIR, "sessions.json");

// ── Types ─────────────────────────────────────────────────────────

export interface AgentInfo {
  key: string;
  sessionId: string;
  model: string;
  modelProvider: string;
  totalTokens: number;
  inputTokens: number;
  outputTokens: number;
  updatedAt: number;
  sessionFile: string;
  isActive: boolean; // updated within last 60s
  thinkingLevel: string;
}

export interface SessionMessage {
  id: string;
  parentId?: string;
  timestamp: string;
  role: "user" | "assistant" | "tool" | "system";
  content: string;
  model?: string;
  tokens?: {
    input: number;
    output: number;
    cacheRead: number;
    cacheWrite: number;
    total: number;
  };
  cost?: number;
  stopReason?: string;
  toolUse?: { name: string; input: any }[];
}

// ── Session Index ─────────────────────────────────────────────────

function readSessionsIndex(): Record<string, any> {
  if (!existsSync(SESSIONS_INDEX)) return {};
  try {
    return JSON.parse(readFileSync(SESSIONS_INDEX, "utf-8"));
  } catch {
    return {};
  }
}

/**
 * List all known agents with summary info.
 */
export function listAgents(): AgentInfo[] {
  const index = readSessionsIndex();
  const now = Date.now();
  const agents: AgentInfo[] = [];

  for (const [key, entry] of Object.entries(index)) {
    const e = entry as any;
    agents.push({
      key,
      sessionId: e.sessionId || "unknown",
      model: e.model || "unknown",
      modelProvider: e.modelProvider || "unknown",
      totalTokens: e.totalTokens || 0,
      inputTokens: e.inputTokens || 0,
      outputTokens: e.outputTokens || 0,
      updatedAt: e.updatedAt || 0,
      sessionFile: e.sessionFile || "",
      isActive: now - (e.updatedAt || 0) < 60_000,
      thinkingLevel: e.thinkingLevel || "medium",
    });
  }

  return agents.sort((a, b) => b.updatedAt - a.updatedAt);
}

// ── Session Transcript Reader ─────────────────────────────────────

function parseJsonlFile(filePath: string): any[] {
  if (!existsSync(filePath)) return [];
  try {
    const content = readFileSync(filePath, "utf-8");
    const lines = content.split("\n").filter((l) => l.trim());
    const records: any[] = [];
    for (const line of lines) {
      try {
        records.push(JSON.parse(line));
      } catch {
        // skip malformed lines
      }
    }
    return records;
  } catch {
    return [];
  }
}

function extractTextContent(content: any): string {
  if (typeof content === "string") return content;
  if (Array.isArray(content)) {
    return content
      .map((block: any) => {
        if (block.type === "text") return block.text || "";
        if (block.type === "tool_use") return `[Tool: ${block.name}]`;
        if (block.type === "tool_result") {
          const inner =
            typeof block.content === "string"
              ? block.content
              : Array.isArray(block.content)
                ? block.content.map((c: any) => c.text || "").join("")
                : "";
          return `[Tool Result: ${inner.slice(0, 200)}${inner.length > 200 ? "..." : ""}]`;
        }
        if (block.type === "thinking") return `[Thinking: ${(block.thinking || "").slice(0, 100)}...]`;
        return "";
      })
      .filter(Boolean)
      .join("\n");
  }
  return "";
}

function extractToolUse(content: any): { name: string; input: any }[] | undefined {
  if (!Array.isArray(content)) return undefined;
  const tools = content
    .filter((b: any) => b.type === "tool_use")
    .map((b: any) => ({ name: b.name || "unknown", input: b.input }));
  return tools.length > 0 ? tools : undefined;
}

/**
 * Read messages from an agent's session transcript.
 */
export function getAgentMessages(
  sessionKey: string,
  limit = 50,
  offset = 0
): { messages: SessionMessage[]; total: number } {
  const index = readSessionsIndex();
  const entry = index[sessionKey] as any;
  if (!entry?.sessionFile) return { messages: [], total: 0 };

  const records = parseJsonlFile(entry.sessionFile);
  const messageRecords = records.filter((r) => r.type === "message" && r.message);

  const total = messageRecords.length;

  // Get the requested slice (from the end for most recent)
  const start = Math.max(0, total - offset - limit);
  const end = total - offset;
  const slice = messageRecords.slice(start, end);

  const messages: SessionMessage[] = slice.map((r) => {
    const msg = r.message;
    const usage = msg.usage;
    return {
      id: r.id || "",
      parentId: r.parentId,
      timestamp: r.timestamp || new Date(msg.timestamp || 0).toISOString(),
      role: msg.role || "user",
      content: extractTextContent(msg.content),
      model: msg.model,
      tokens: usage
        ? {
            input: usage.input || 0,
            output: usage.output || 0,
            cacheRead: usage.cacheRead || 0,
            cacheWrite: usage.cacheWrite || 0,
            total: usage.totalTokens || 0,
          }
        : undefined,
      cost: usage?.cost?.total,
      stopReason: msg.stopReason,
      toolUse: extractToolUse(msg.content),
    };
  });

  return { messages, total };
}

/**
 * Get agent status — what it's currently doing.
 */
export function getAgentStatus(sessionKey: string): {
  active: boolean;
  sessionId: string;
  model: string;
  totalTokens: number;
  lastMessage: string;
  lastRole: string;
  lastTimestamp: string;
  currentTask: string;
} {
  const index = readSessionsIndex();
  const entry = index[sessionKey] as any;
  if (!entry) {
    return {
      active: false,
      sessionId: "unknown",
      model: "unknown",
      totalTokens: 0,
      lastMessage: "",
      lastRole: "",
      lastTimestamp: "",
      currentTask: "No active session",
    };
  }

  const now = Date.now();
  const active = now - (entry.updatedAt || 0) < 60_000;

  // Read last few messages to determine current task
  const { messages } = getAgentMessages(sessionKey, 5, 0);
  const lastMsg = messages[messages.length - 1];
  const lastUserMsg = [...messages].reverse().find((m) => m.role === "user");

  // Try to extract what the agent is working on from recent messages
  let currentTask = "Idle";
  if (active && lastUserMsg) {
    const text = lastUserMsg.content;
    if (text.toLowerCase().includes("build")) {
      const appMatch = text.match(/called "([^"]+)"/i) || text.match(/app (\S+)/i);
      currentTask = appMatch ? `Building ${appMatch[1]}` : "Building an app";
    } else {
      currentTask = text.slice(0, 120) + (text.length > 120 ? "..." : "");
    }
  }

  return {
    active,
    sessionId: entry.sessionId || "unknown",
    model: entry.model || "unknown",
    totalTokens: entry.totalTokens || 0,
    lastMessage: lastMsg?.content?.slice(0, 200) || "",
    lastRole: lastMsg?.role || "",
    lastTimestamp: lastMsg?.timestamp || "",
    currentTask,
  };
}

/**
 * Get builds for a specific agent (from events store).
 */
export function getAgentBuilds(agentId: string, events: any[]): any[] {
  return events
    .filter((e) => e.type === "build" && e.agentId === agentId)
    .map((e) => ({
      appName: e.data?.appName,
      success: e.data?.success,
      apkSize: e.data?.apkSize,
      timestamp: e.timestamp,
      runId: e.data?.runId,
    }));
}

/**
 * Get cost breakdown for a specific agent.
 */
export function getAgentCostBreakdown(agentId: string, events: any[]) {
  let inputTokens = 0,
    outputTokens = 0,
    cacheRead = 0,
    cacheWrite = 0,
    totalCost = 0;
  const byModel: Record<string, { cost: number; tokens: number; calls: number }> = {};

  for (const e of events) {
    if (e.type !== "cost" || e.agentId !== agentId) continue;
    const d = e.data;
    inputTokens += d.inputTokens || 0;
    outputTokens += d.outputTokens || 0;
    cacheRead += d.cacheRead || 0;
    cacheWrite += d.cacheWrite || 0;
    totalCost += d.costUsd || 0;

    const model = d.model || "unknown";
    if (!byModel[model]) byModel[model] = { cost: 0, tokens: 0, calls: 0 };
    byModel[model].cost += d.costUsd || 0;
    byModel[model].tokens += (d.inputTokens || 0) + (d.outputTokens || 0);
    byModel[model].calls += 1;
  }

  return { inputTokens, outputTokens, cacheRead, cacheWrite, totalCost, byModel };
}

// ── SSE Live Stream ───────────────────────────────────────────────

const sseClients = new Map<string, Set<Response>>();

/**
 * Register an SSE client for an agent's live stream.
 */
export function addSSEClient(sessionKey: string, res: Response) {
  if (!sseClients.has(sessionKey)) sseClients.set(sessionKey, new Set());
  sseClients.get(sessionKey)!.add(res);

  res.on("close", () => {
    sseClients.get(sessionKey)?.delete(res);
  });
}

/**
 * Start watching a session file for changes and push updates to SSE clients.
 */
const watchedFiles = new Set<string>();

export function watchSessionFile(sessionKey: string) {
  const index = readSessionsIndex();
  const entry = index[sessionKey] as any;
  if (!entry?.sessionFile || watchedFiles.has(entry.sessionFile)) return;

  const filePath = entry.sessionFile;
  if (!existsSync(filePath)) return;

  let lastSize = statSync(filePath).size;
  watchedFiles.add(filePath);

  watchFile(filePath, { interval: 1000 }, () => {
    try {
      const newSize = statSync(filePath).size;
      if (newSize <= lastSize) return;

      // Read only the new bytes
      const fd = require("fs").openSync(filePath, "r");
      const buffer = Buffer.alloc(newSize - lastSize);
      require("fs").readSync(fd, buffer, 0, buffer.length, lastSize);
      require("fs").closeSync(fd);
      lastSize = newSize;

      const newLines = buffer.toString("utf-8").split("\n").filter((l: string) => l.trim());
      for (const line of newLines) {
        try {
          const record = JSON.parse(line);
          if (record.type !== "message") continue;

          const msg = record.message;
          const data: SessionMessage = {
            id: record.id || "",
            parentId: record.parentId,
            timestamp: record.timestamp || new Date(msg.timestamp || 0).toISOString(),
            role: msg.role,
            content: extractTextContent(msg.content),
            model: msg.model,
            tokens: msg.usage
              ? {
                  input: msg.usage.input || 0,
                  output: msg.usage.output || 0,
                  cacheRead: msg.usage.cacheRead || 0,
                  cacheWrite: msg.usage.cacheWrite || 0,
                  total: msg.usage.totalTokens || 0,
                }
              : undefined,
            cost: msg.usage?.cost?.total,
            stopReason: msg.stopReason,
            toolUse: extractToolUse(msg.content),
          };

          // Push to all SSE clients watching this session
          const clients = sseClients.get(sessionKey);
          if (clients) {
            const payload = `data: ${JSON.stringify(data)}\n\n`;
            for (const client of clients) {
              client.write(payload);
            }
          }
        } catch {}
      }
    } catch {}
  });
}

/**
 * List all available session files (for browsing past sessions).
 */
export function listSessionFiles(): { name: string; size: number; modified: string }[] {
  if (!existsSync(SESSIONS_DIR)) return [];
  return readdirSync(SESSIONS_DIR)
    .filter((f) => f.endsWith(".jsonl"))
    .map((f) => {
      const fullPath = join(SESSIONS_DIR, f);
      const stat = statSync(fullPath);
      return {
        name: f.replace(".jsonl", ""),
        size: stat.size,
        modified: stat.mtime.toISOString(),
      };
    })
    .sort((a, b) => new Date(b.modified).getTime() - new Date(a.modified).getTime());
}
