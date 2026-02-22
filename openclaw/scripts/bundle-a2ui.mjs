#!/usr/bin/env node
/**
 * A2UI bundle script - Node.js version for Windows compatibility.
 * Replaces bundle-a2ui.sh when bash/node PATH issues occur.
 */
import { createHash } from "node:crypto";
import { existsSync, readFileSync, readdirSync, statSync, writeFileSync } from "node:fs";
import { dirname, join, relative } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT_DIR = join(__dirname, "..");
const HASH_FILE = join(ROOT_DIR, "src/canvas-host/a2ui/.bundle.hash");
const OUTPUT_FILE = join(ROOT_DIR, "src/canvas-host/a2ui/a2ui.bundle.js");
const A2UI_RENDERER_DIR = join(ROOT_DIR, "vendor/a2ui/renderers/lit");
const A2UI_APP_DIR = join(ROOT_DIR, "apps/shared/OpenClawKit/Tools/CanvasA2UI");

if (!existsSync(A2UI_RENDERER_DIR) || !existsSync(A2UI_APP_DIR)) {
  if (existsSync(OUTPUT_FILE)) {
    console.log("A2UI sources missing; keeping prebuilt bundle.");
    process.exit(0);
  }
  console.error("A2UI sources missing and no prebuilt bundle found at:", OUTPUT_FILE);
  process.exit(1);
}

const INPUT_PATHS = [
  join(ROOT_DIR, "package.json"),
  join(ROOT_DIR, "pnpm-lock.yaml"),
  A2UI_RENDERER_DIR,
  A2UI_APP_DIR,
];

function walk(entryPath, files = []) {
  const st = statSync(entryPath);
  if (st.isDirectory()) {
    for (const entry of readdirSync(entryPath)) {
      walk(join(entryPath, entry), files);
    }
    return files;
  }
  files.push(entryPath);
  return files;
}

function computeHash() {
  const files = [];
  for (const input of INPUT_PATHS) {
    walk(input, files);
  }
  files.sort((a, b) => a.localeCompare(b));
  const hash = createHash("sha256");
  for (const filePath of files) {
    const rel = relative(ROOT_DIR, filePath).replace(/\\/g, "/");
    hash.update(rel);
    hash.update("\0");
    hash.update(readFileSync(filePath));
    hash.update("\0");
  }
  return hash.digest("hex");
}

const currentHash = computeHash();
let previousHash = "";
if (existsSync(HASH_FILE)) {
  previousHash = readFileSync(HASH_FILE, "utf8").trim();
  if (previousHash === currentHash && existsSync(OUTPUT_FILE)) {
    console.log("A2UI bundle up to date; skipping.");
    process.exit(0);
  }
}

// Run tsc and rolldown via spawn
import { spawnSync } from "node:child_process";

const tscResult = spawnSync("pnpm", ["-s", "exec", "tsc", "-p", join(A2UI_RENDERER_DIR, "tsconfig.json")], {
  stdio: "inherit",
  cwd: ROOT_DIR,
  shell: true,
});
if (tscResult.status !== 0) {
  console.error("A2UI bundling failed. Re-run with: pnpm canvas:a2ui:bundle");
  process.exit(1);
}

const rolldownResult = spawnSync("pnpm", ["-s", "dlx", "rolldown", "-c", join(A2UI_APP_DIR, "rolldown.config.mjs")], {
  stdio: "inherit",
  cwd: ROOT_DIR,
  shell: true,
});
if (rolldownResult.status !== 0) {
  console.error("A2UI bundling failed. Re-run with: pnpm canvas:a2ui:bundle");
  process.exit(1);
}

writeFileSync(HASH_FILE, currentHash);
console.log("A2UI bundle complete.");
