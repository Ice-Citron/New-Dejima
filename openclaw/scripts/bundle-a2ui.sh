#!/usr/bin/env bash
set -euo pipefail

# Ensure Node is on PATH when running from Windows (WSL / Git Bash / MSYS)
for p in "/mnt/c/Program Files/nodejs" "/c/Program Files/nodejs"; do
  if [[ -f "$p/node.exe" ]]; then
    export PATH="$p:$PATH"
    break
  fi
done

on_error() {
  echo "A2UI bundling failed. Re-run with: pnpm canvas:a2ui:bundle" >&2
  echo "If this persists, verify pnpm deps and try again." >&2
}
trap on_error ERR

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
HASH_FILE="$ROOT_DIR/src/canvas-host/a2ui/.bundle.hash"
OUTPUT_FILE="$ROOT_DIR/src/canvas-host/a2ui/a2ui.bundle.js"
A2UI_RENDERER_DIR="$ROOT_DIR/vendor/a2ui/renderers/lit"
A2UI_APP_DIR="$ROOT_DIR/apps/shared/OpenClawKit/Tools/CanvasA2UI"

# Docker builds exclude vendor/apps via .dockerignore.
# In that environment we can keep a prebuilt bundle only if it exists.
if [[ ! -d "$A2UI_RENDERER_DIR" || ! -d "$A2UI_APP_DIR" ]]; then
  if [[ -f "$OUTPUT_FILE" ]]; then
    echo "A2UI sources missing; keeping prebuilt bundle."
    exit 0
  fi
  echo "A2UI sources missing and no prebuilt bundle found at: $OUTPUT_FILE" >&2
  exit 1
fi

INPUT_PATHS=(
  "$ROOT_DIR/package.json"
  "$ROOT_DIR/pnpm-lock.yaml"
  "$A2UI_RENDERER_DIR"
  "$A2UI_APP_DIR"
)

# Resolve node: use Windows path when in WSL and node not in PATH
resolve_node() {
  if command -v node >/dev/null 2>&1; then
    echo "node"
    return
  fi
  for p in "/mnt/c/Program Files/nodejs/node.exe" "/c/Program Files/nodejs/node.exe"; do
    [[ -f "$p" ]] && echo "$p" && return
  done
  echo "node"
}
NODE="$(resolve_node)"

# Convert WSL/Unix paths to Windows when using Windows Node
to_win_path() {
  local p="$1"
  if [[ "$NODE" == *".exe" ]]; then
    if command -v wslpath >/dev/null 2>&1; then
      wslpath -w "$p" 2>/dev/null || echo "$p"
    else
      echo "$p" | sed 's|^/mnt/c/|C:/|;s|^/c/|C:/|;s|/|\\|g'
    fi
  else
    echo "$p"
  fi
}
ROOT_DIR_WIN="$(to_win_path "$ROOT_DIR")"
INPUT_PATHS_WIN=()
for p in "${INPUT_PATHS[@]}"; do
  INPUT_PATHS_WIN+=("$(to_win_path "$p")")
done

compute_hash() {
  ROOT_DIR="$ROOT_DIR_WIN" "$NODE" --input-type=module - "${INPUT_PATHS_WIN[@]}" <<'NODE'
import { createHash } from "node:crypto";
import { promises as fs } from "node:fs";
import path from "node:path";

const rootDir = process.env.ROOT_DIR ?? process.cwd();
const inputs = process.argv.slice(2);
const files = [];

async function walk(entryPath) {
  const st = await fs.stat(entryPath);
  if (st.isDirectory()) {
    const entries = await fs.readdir(entryPath);
    for (const entry of entries) {
      await walk(path.join(entryPath, entry));
    }
    return;
  }
  files.push(entryPath);
}

for (const input of inputs) {
  await walk(input);
}

function normalize(p) {
  return p.split(path.sep).join("/");
}

files.sort((a, b) => normalize(a).localeCompare(normalize(b)));

const hash = createHash("sha256");
for (const filePath of files) {
  const rel = normalize(path.relative(rootDir, filePath));
  hash.update(rel);
  hash.update("\0");
  hash.update(await fs.readFile(filePath));
  hash.update("\0");
}

process.stdout.write(hash.digest("hex"));
NODE
}

current_hash="$(compute_hash)"
if [[ -f "$HASH_FILE" ]]; then
  previous_hash="$(cat "$HASH_FILE")"
  if [[ "$previous_hash" == "$current_hash" && -f "$OUTPUT_FILE" ]]; then
    echo "A2UI bundle up to date; skipping."
    exit 0
  fi
fi

# Ensure node is available for pnpm (Windows/WSL)
NODE_DIR=""
for p in "/mnt/c/Program Files/nodejs" "/c/Program Files/nodejs"; do
  [[ -f "$p/node.exe" ]] && NODE_DIR="$p" && break
done
[[ -n "$NODE_DIR" ]] && export PATH="$NODE_DIR:$PATH"

pnpm -s exec tsc -p "$A2UI_RENDERER_DIR/tsconfig.json"
if command -v rolldown >/dev/null 2>&1; then
  rolldown -c "$A2UI_APP_DIR/rolldown.config.mjs"
else
  pnpm -s dlx rolldown -c "$A2UI_APP_DIR/rolldown.config.mjs"
fi

echo "$current_hash" > "$HASH_FILE"
