#!/bin/bash
# ============================================================
# AI Video Creator — Full Autonomous Pipeline
#
# Usage:
#   bash run-pipeline.sh "App name — description" [landscape|portrait|both]
#
# Options (env vars):
#   APP_SOURCE_DIR=/path/to/app   — read app source code to auto-generate creative brief
#   APP_BRIEF="..."               — provide creative brief directly (overrides APP_SOURCE_DIR)
#   VOICE_STYLE=male|female|random — default: random
#   ELEVENLABS_VOICE_ID=<id>      — force a specific voice (default: random from pool)
#
# Examples:
#   bash run-pipeline.sh "Golf Deals — finds the best golf balls"
#   APP_SOURCE_DIR=/path/to/myapp bash run-pipeline.sh "MyApp — description"
#   VOICE_STYLE=female bash run-pipeline.sh "MyApp — description" portrait
# ============================================================
set -a
source /Users/yacine/Desktop/Dejima2/New-Dejima/.env
set +a

export TOPIC="${1:-Golf Deals — finds the best golf balls for your criteria}"
export FORMAT="${2:-portrait}"   # landscape | portrait | both

# Voice: leave blank so pipeline picks randomly unless user overrides
export ELEVENLABS_VOICE_ID="${ELEVENLABS_VOICE_ID:-}"
export VOICE_STYLE="${VOICE_STYLE:-random}"   # male | female | random

# App analysis: set APP_SOURCE_DIR to auto-generate creative brief from code
export APP_SOURCE_DIR="${APP_SOURCE_DIR:-}"
export APP_BRIEF="${APP_BRIEF:-}"

PIPELINE="python3 /Users/yacine/Desktop/Dejima2/New-Dejima/config/openclaw/skills/ai-video-creator/scripts/pipeline.py"

if [ "$FORMAT" = "both" ]; then
  echo "▶ Running LANDSCAPE (16:9 YouTube)..."
  export FORMAT="landscape"
  export VIDEO_OUTPUT_DIR="/tmp/dejima-videos/$(date +%Y%m%d_%H%M%S)_landscape"
  mkdir -p "$VIDEO_OUTPUT_DIR"
  $PIPELINE

  echo ""
  echo "▶ Running PORTRAIT (9:16 Shorts)..."
  export FORMAT="portrait"
  export VIDEO_OUTPUT_DIR="/tmp/dejima-videos/$(date +%Y%m%d_%H%M%S)_portrait"
  mkdir -p "$VIDEO_OUTPUT_DIR"
  $PIPELINE
else
  export VIDEO_OUTPUT_DIR="/tmp/dejima-videos/$(date +%Y%m%d_%H%M%S)_${FORMAT}"
  mkdir -p "$VIDEO_OUTPUT_DIR"
  $PIPELINE
fi
