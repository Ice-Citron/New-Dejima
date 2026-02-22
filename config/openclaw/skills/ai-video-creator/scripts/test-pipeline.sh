#!/bin/bash
# ============================================================
# AI Video Creator — Full Pipeline Test
# Run this to verify every component works end-to-end
# Usage: bash test-pipeline.sh
# ============================================================

set -a
source /Users/yacine/Desktop/Dejima2/New-Dejima/.env
set +a

export VIDEO_OUTPUT_DIR="${VIDEO_OUTPUT_DIR:-/tmp/dejima-videos}"
mkdir -p "$VIDEO_OUTPUT_DIR"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

pass() { echo -e "${GREEN}✓ $1${NC}"; }
fail() { echo -e "${RED}✗ $1${NC}"; }
info() { echo -e "${BLUE}→ $1${NC}"; }
warn() { echo -e "${YELLOW}⚠ $1${NC}"; }

echo ""
echo "=============================================="
echo "   AI VIDEO CREATOR — PIPELINE TEST"
echo "=============================================="
echo ""

# ---- Test 1: Environment ----
echo "[ 1/6 ] Checking environment..."

[ -n "$GEMINI_API_KEY" ] && pass "GEMINI_API_KEY set" || fail "GEMINI_API_KEY missing — add to .env"
[ -n "$ELEVENLABS_API_KEY" ] && pass "ELEVENLABS_API_KEY set" || fail "ELEVENLABS_API_KEY missing — add to .env"
[ -n "$ELEVENLABS_VOICE_ID" ] && pass "ELEVENLABS_VOICE_ID set" || warn "ELEVENLABS_VOICE_ID not set — using default Rachel"
export ELEVENLABS_VOICE_ID="${ELEVENLABS_VOICE_ID:-21m00Tcm4TlvDq8ikWAM}"

which ffmpeg > /dev/null && pass "ffmpeg installed ($(ffmpeg -version 2>&1 | head -1 | awk '{print $3}'))" || fail "ffmpeg not found"
which python3 > /dev/null && pass "python3 installed ($(python3 --version))" || fail "python3 not found"

echo ""

# ---- Test 2: Gemini / Veo 3 ----
echo "[ 2/6 ] Testing Gemini API (Veo 3 + Imagen 4)..."

info "Checking available models..."
MODEL_COUNT=$(curl -s "https://generativelanguage.googleapis.com/v1beta/models?key=$GEMINI_API_KEY" \
  | python3 -c "import json,sys; d=json.loads(sys.stdin.read()); print(len(d.get('models',[])))" 2>/dev/null)

if [ "$MODEL_COUNT" -gt 0 ] 2>/dev/null; then
  pass "Gemini API key valid ($MODEL_COUNT models available)"
  VEO_AVAILABLE=$(curl -s "https://generativelanguage.googleapis.com/v1beta/models?key=$GEMINI_API_KEY" \
    | python3 -c "import json,sys; d=json.loads(sys.stdin.read()); veo=[m['name'] for m in d.get('models',[]) if 'veo' in m['name']]; print(', '.join(veo))" 2>/dev/null)
  pass "Veo models: $VEO_AVAILABLE"
else
  fail "Gemini API key invalid or quota exceeded"
fi

echo ""

# ---- Test 3: ElevenLabs ----
echo "[ 3/6 ] Testing ElevenLabs TTS..."

info "Generating test audio clip..."
TTS_STATUS=$(curl -s -o "$VIDEO_OUTPUT_DIR/tts_test.mp3" -w "%{http_code}" \
  -X POST "https://api.elevenlabs.io/v1/text-to-speech/$ELEVENLABS_VOICE_ID" \
  -H "xi-api-key: $ELEVENLABS_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"text": "Dejima AI video pipeline test. All systems operational.", "model_id": "eleven_flash_v2_5", "voice_settings": {"stability": 0.5, "similarity_boost": 0.75}}')

if [ "$TTS_STATUS" = "200" ]; then
  AUDIO_DURATION=$(ffprobe -v quiet -print_format json -show_format "$VIDEO_OUTPUT_DIR/tts_test.mp3" 2>/dev/null \
    | python3 -c "import json,sys; print(f\"{float(json.load(sys.stdin)['format']['duration']):.1f}\")" 2>/dev/null)
  pass "ElevenLabs TTS working — ${AUDIO_DURATION}s audio generated"
else
  TTS_ERROR=$(cat "$VIDEO_OUTPUT_DIR/tts_test.mp3" 2>/dev/null | python3 -c "import json,sys; d=json.loads(sys.stdin.read()); print(d.get('detail',{}).get('message','Unknown error'))" 2>/dev/null)
  fail "ElevenLabs TTS failed (HTTP $TTS_STATUS): $TTS_ERROR"
fi

echo ""

# ---- Test 4: Veo 3 Video Generation ----
echo "[ 4/6 ] Testing Veo 3 video generation (takes ~30-60s)..."

info "Starting video generation job..."
OPERATION=$(curl -s -X POST \
  "https://generativelanguage.googleapis.com/v1beta/models/veo-3.0-generate-001:predictLongRunning" \
  -H "x-goog-api-key: $GEMINI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"instances": [{"prompt": "Abstract digital technology background with glowing blue particles, cinematic 4K, professional"}], "parameters": {"aspectRatio": "16:9", "sampleCount": 1}}')

OPERATION_NAME=$(echo "$OPERATION" | python3 -c "import json,sys; d=json.loads(sys.stdin.read()); print(d.get('name','ERROR'))" 2>/dev/null)

if [[ "$OPERATION_NAME" == models/* ]]; then
  pass "Video generation job started: $OPERATION_NAME"
  info "Polling for completion..."

  for i in $(seq 1 30); do
    sleep 10
    STATUS=$(curl -s -H "x-goog-api-key: $GEMINI_API_KEY" \
      "https://generativelanguage.googleapis.com/v1beta/${OPERATION_NAME}")
    IS_DONE=$(echo "$STATUS" | python3 -c "import json,sys; print(json.loads(sys.stdin.read()).get('done', False))" 2>/dev/null)

    if [ "$IS_DONE" = "True" ]; then
      VIDEO_URI=$(echo "$STATUS" | python3 -c "
import json, sys
d = json.loads(sys.stdin.read())
try:
    print(d['response']['generateVideoResponse']['generatedSamples'][0]['video']['uri'])
except:
    print('NO_URI')
" 2>/dev/null)

      if [[ "$VIDEO_URI" != "NO_URI" ]]; then
        curl -s -L -o "$VIDEO_OUTPUT_DIR/test_veo.mp4" \
          -H "x-goog-api-key: $GEMINI_API_KEY" "$VIDEO_URI"

        VEO_DURATION=$(ffprobe -v quiet -print_format json -show_format "$VIDEO_OUTPUT_DIR/test_veo.mp4" 2>/dev/null \
          | python3 -c "import json,sys; print(f\"{float(json.load(sys.stdin)['format']['duration']):.1f}\")" 2>/dev/null)
        VEO_SIZE=$(wc -c < "$VIDEO_OUTPUT_DIR/test_veo.mp4" | xargs)

        pass "Veo 3 video generated: ${VEO_DURATION}s, $((VEO_SIZE/1000))KB"
      else
        fail "Video generation completed but no URI in response"
        echo "$STATUS" | python3 -m json.tool | head -20
      fi
      break
    fi
    echo "  Still generating... ($((i*10))s)"
  done
else
  fail "Failed to start video generation: $OPERATION"
fi

echo ""

# ---- Test 5: Imagen 4 Thumbnail ----
echo "[ 5/6 ] Testing Imagen 4 thumbnail generation..."

THUMB_RESPONSE=$(curl -s -X POST \
  "https://generativelanguage.googleapis.com/v1beta/models/imagen-4.0-generate-001:predict" \
  -H "x-goog-api-key: $GEMINI_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"instances": [{"prompt": "Bold YouTube thumbnail, technology theme, dark background, bright colors, professional"}], "parameters": {"sampleCount": 1, "aspectRatio": "16:9"}}')

THUMB_OK=$(echo "$THUMB_RESPONSE" | python3 -c "
import json, sys, base64
d = json.loads(sys.stdin.read())
if 'predictions' in d:
    with open('$VIDEO_OUTPUT_DIR/test_thumb.jpg', 'wb') as f:
        f.write(base64.b64decode(d['predictions'][0]['bytesBase64Encoded']))
    import os
    print(os.path.getsize('$VIDEO_OUTPUT_DIR/test_thumb.jpg'))
else:
    print('ERROR: ' + str(d)[:100])
" 2>/dev/null)

if [[ "$THUMB_OK" =~ ^[0-9]+$ ]]; then
  pass "Imagen 4 thumbnail generated: $((THUMB_OK/1000))KB"
else
  fail "Imagen 4 failed: $THUMB_OK"
fi

echo ""

# ---- Test 6: ffmpeg merge ----
echo "[ 6/6 ] Testing ffmpeg audio+video merge..."

if [ -f "$VIDEO_OUTPUT_DIR/test_veo.mp4" ] && [ -f "$VIDEO_OUTPUT_DIR/tts_test.mp3" ]; then
  ffmpeg -y -loglevel error \
    -i "$VIDEO_OUTPUT_DIR/test_veo.mp4" \
    -i "$VIDEO_OUTPUT_DIR/tts_test.mp3" \
    -c:v copy -c:a aac -b:a 192k \
    -map 0:v:0 -map 1:a:0 \
    -shortest \
    "$VIDEO_OUTPUT_DIR/test_merged.mp4" 2>&1

  if [ -f "$VIDEO_OUTPUT_DIR/test_merged.mp4" ] && [ "$(wc -c < "$VIDEO_OUTPUT_DIR/test_merged.mp4")" -gt 50000 ]; then
    MERGED_INFO=$(ffprobe -v quiet -print_format json -show_format -show_streams "$VIDEO_OUTPUT_DIR/test_merged.mp4" 2>/dev/null \
      | python3 -c "
import json,sys
d=json.loads(sys.stdin.read())
dur=float(d['format']['duration'])
v=next((s for s in d['streams'] if s['codec_type']=='video'),{})
a=next((s for s in d['streams'] if s['codec_type']=='audio'),{})
print(f'{dur:.1f}s | {v.get(\"width\")}x{v.get(\"height\")} | audio: {a.get(\"codec_name\",\"none\")}')
" 2>/dev/null)
    pass "ffmpeg merge working: $MERGED_INFO"
  else
    fail "ffmpeg merge failed or output too small"
  fi
else
  warn "Skipping merge test — Veo or TTS test failed earlier"
fi

echo ""
echo "=============================================="
echo "   RESULTS"
echo "=============================================="
echo ""
echo "Test artifacts saved to: $VIDEO_OUTPUT_DIR"
ls -lh "$VIDEO_OUTPUT_DIR"/ 2>/dev/null
echo ""

if [ -n "$YOUTUBE_REFRESH_TOKEN" ]; then
  pass "YouTube: configured"
else
  warn "YouTube: not configured yet (follow docs/plans/api-setup-guides.md)"
fi

if [ -n "$TIKTOK_ACCESS_TOKEN" ]; then
  pass "TikTok: configured"
else
  warn "TikTok: not configured yet"
fi

echo ""
echo "Run the full pipeline: ask the OpenClaw agent to use the ai-video-creator skill"
echo ""
