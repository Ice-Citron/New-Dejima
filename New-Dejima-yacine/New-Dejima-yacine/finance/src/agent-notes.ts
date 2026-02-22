/**
 * Agent notes — persistent store for inter-generational context.
 *
 * Agents write notes for their children or future selves.
 * Persisted to notes.json alongside wallets.json.
 */
import { readFileSync, writeFileSync, existsSync } from "fs";

const NOTES_PATH = new URL("../../notes.json", import.meta.url).pathname;

export interface AgentNote {
  fromAgentId: string;
  content: string;
  writtenAt: string;
  forChild: boolean;  // true = for next generation, false = for future self
}

type NotesStore = Record<string, AgentNote[]>;

function loadStore(): NotesStore {
  if (!existsSync(NOTES_PATH)) return {};
  return JSON.parse(readFileSync(NOTES_PATH, "utf-8"));
}

function saveStore(store: NotesStore) {
  writeFileSync(NOTES_PATH, JSON.stringify(store, null, 2));
}

export function writeNote(
  agentId: string,
  content: string,
  forChild: boolean = true
): AgentNote {
  const note: AgentNote = {
    fromAgentId: agentId,
    content,
    writtenAt: new Date().toISOString(),
    forChild,
  };
  const store = loadStore();
  if (!store[agentId]) store[agentId] = [];
  store[agentId].push(note);
  saveStore(store);
  return note;
}

/** All notes written by a specific agent */
export function readNotes(agentId: string): AgentNote[] {
  const store = loadStore();
  return store[agentId] ?? [];
}

/** Notes left by a parent for their child (forChild=true) */
export function readChildNotes(parentId: string): AgentNote[] {
  return readNotes(parentId).filter((n) => n.forChild);
}
