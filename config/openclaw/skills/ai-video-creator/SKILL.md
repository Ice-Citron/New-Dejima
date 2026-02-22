---
name: ai-video-creator
description: Autonomously generate, narrate, render, and publish videos to YouTube, TikTok, and Instagram. Uses Claude for scripting, ElevenLabs for voice, Veo 2 for video, ffmpeg for post-production.
---

# AI Video Creator

You are an autonomous video content creator. Given a topic or brief, you produce a fully published video with zero human input. Follow this exact 7-phase pipeline. NEVER skip a phase. NEVER proceed past a HARD GATE without success.

## Environment Variables Required

These must be set before running any phase:
```
GEMINI_API_KEY        — Google AI API key (for Veo 2 + Imagen 4)
ELEVENLABS_API_KEY    — ElevenLabs API key
ELEVENLABS_VOICE_ID   — Voice ID (default: 21m00Tcm4TlvDq8ikWAM = Rachel)
YOUTUBE_CLIENT_ID     — YouTube OAuth client ID
YOUTUBE_CLIENT_SECRET — YouTube OAuth client secret
YOUTUBE_REFRESH_TOKEN — Pre-authorized refresh token
VIDEO_OUTPUT_DIR      — Working directory (default: /tmp/dejima-videos)
```

Load them at start:
```bash
set -a && source /Users/yacine/Desktop/Dejima2/New-Dejima/.env && set +a
export VIDEO_OUTPUT_DIR="${VIDEO_OUTPUT_DIR:-/tmp/dejima-videos}"
export ELEVENLABS_VOICE_ID="${ELEVENLABS_VOICE_ID:-21m00Tcm4TlvDq8ikWAM}"
mkdir -p "$VIDEO_OUTPUT_DIR"
```

## Input Formats

You accept three types of input:

1. **Topic brief**: `"Create a video about Bitcoin reaching $100K"`
2. **App trailer**: `"Create a trailer for [app_name] — [one-line description]"`
3. **Format override**: `"Create a TikTok short about [topic]"` or `"Create a YouTube explainer about [topic]"`

Determine format automatically if not specified:
- App trailers → 60s, portrait (9:16) for TikTok + Reels, landscape (16:9) for YouTube
- Tech/finance topics → 90-180s YouTube + 60s TikTok cut
- Breaking news → 30-60s all platforms

---

## Phase 1: Script Generation

**Goal**: Produce a complete shooting script from the topic.

Run this exact prompt against Claude (call yourself recursively via the API, or generate inline):

Write a video script for: `[TOPIC]`

Format output as a JSON file at `$VIDEO_OUTPUT_DIR/script.json`:

```json
{
  "title": "Catchy YouTube title (max 70 chars)",
  "description": "YouTube description with key points (200-300 chars)",
  "tags": ["tag1", "tag2", "tag3", "tag4", "tag5"],
  "format": "youtube_long | tiktok_short | both",
  "duration_target_seconds": 90,
  "narration": "Full narration text, written to be spoken aloud. Natural pacing. No bullet points — full sentences only. Approx 150 words per minute.",
  "scenes": [
    {
      "id": 1,
      "duration_seconds": 5,
      "narration_segment": "The words spoken during this scene",
      "video_prompt": "Detailed visual description for Veo 2. Cinematic style. Example: 'Close-up of golden Bitcoin coin spinning in dramatic lighting, dark background, 4K cinematic, slow motion'"
    }
  ],
  "thumbnail_prompt": "Imagen 4 prompt for thumbnail image. Eye-catching. Bold text space on left side."
}
```

Rules for video_prompt per scene:
- Always specify: camera angle, subject, lighting, style, movement
- Append: `, cinematic 4K, professional`
- Keep each scene 5-8 seconds (Veo 2 limit)
- No copyrighted brand logos or real people by name

Save the file. Read it back to verify valid JSON before proceeding.

**GATE**: `script.json` exists and is valid JSON with at least 3 scenes.

---

## Phase 2: Voice Generation (ElevenLabs)

**Goal**: Convert narration text to a professional MP3.

```bash
# Read narration from script
NARRATION=$(python3 -c "import json; d=open('$VIDEO_OUTPUT_DIR/script.json').read(); print(json.loads(d)['narration'])")

# Call ElevenLabs TTS
curl -s -X POST "https://api.elevenlabs.io/v1/text-to-speech/$ELEVENLABS_VOICE_ID" \
  -H "xi-api-key: $ELEVENLABS_API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"text\": $(echo "$NARRATION" | python3 -c "import json,sys; print(json.dumps(sys.stdin.read()))"),
    \"model_id\": \"eleven_flash_v2_5\",
    \"voice_settings\": {
      \"stability\": 0.5,
      \"similarity_boost\": 0.75,
      \"style\": 0.3,
      \"use_speaker_boost\": true
    }
  }" \
  --output "$VIDEO_OUTPUT_DIR/narration.mp3"
```

Verify the output:
```bash
ffprobe -v quiet -print_format json -show_format "$VIDEO_OUTPUT_DIR/narration.mp3" | python3 -c "
import json, sys
d = json.load(sys.stdin)
duration = float(d['format']['duration'])
size = int(d['format']['size'])
print(f'Duration: {duration:.1f}s, Size: {size} bytes')
assert duration > 5, 'Audio too short'
assert size > 10000, 'File too small — likely API error'
print('GATE PASSED')
"
```

If gate fails: check API key, check narration length (ElevenLabs has a char limit of ~5000 per call — split and concatenate if needed).

**GATE**: `narration.mp3` exists, duration > 5s, file size > 10KB.

---

## Phase 3: Video Scene Generation (Veo 2 / Gemini)

**Goal**: Generate one video clip per scene in parallel.

Read scene count and prompts:
```bash
SCENE_COUNT=$(python3 -c "import json; d=json.load(open('$VIDEO_OUTPUT_DIR/script.json')); print(len(d['scenes']))")
```

For each scene, launch generation:
```bash
BASE_URL="https://generativelanguage.googleapis.com/v1beta"

generate_scene() {
  local SCENE_ID=$1
  local PROMPT=$2
  local DURATION=$3
  local OUT_FILE="$VIDEO_OUTPUT_DIR/scene_$(printf '%03d' $SCENE_ID).mp4"

  # Start async video generation (Veo 3.0 — best quality; fallback to veo-2.0-generate-001 if quota exceeded)
  OPERATION=$(curl -s -X POST \
    "${BASE_URL}/models/veo-3.0-generate-001:predictLongRunning" \
    -H "x-goog-api-key: $GEMINI_API_KEY" \
    -H "Content-Type: application/json" \
    -d "{
      \"instances\": [{\"prompt\": $(echo "$PROMPT" | python3 -c "import json,sys; print(json.dumps(sys.stdin.read().strip()))")}],
      \"parameters\": {
        \"aspectRatio\": \"16:9\",
        \"sampleCount\": 1
      }
    }")

  OPERATION_NAME=$(echo "$OPERATION" | python3 -c "import json,sys; print(json.loads(sys.stdin.read())['name'])")
  echo "Scene $SCENE_ID started: $OPERATION_NAME"

  # Poll until done (max 10 min)
  for i in $(seq 1 60); do
    sleep 10
    STATUS=$(curl -s -H "x-goog-api-key: $GEMINI_API_KEY" "${BASE_URL}/${OPERATION_NAME}")
    IS_DONE=$(echo "$STATUS" | python3 -c "import json,sys; print(json.loads(sys.stdin.read()).get('done', False))")

    if [ "$IS_DONE" = "True" ]; then
      VIDEO_URI=$(echo "$STATUS" | python3 -c "
import json, sys
d = json.loads(sys.stdin.read())
print(d['response']['generateVideoResponse']['generatedSamples'][0]['video']['uri'])
")
      curl -L -o "$OUT_FILE" -H "x-goog-api-key: $GEMINI_API_KEY" "$VIDEO_URI"
      echo "Scene $SCENE_ID downloaded: $OUT_FILE"
      return 0
    fi
    echo "Scene $SCENE_ID: still generating... ($((i*10))s elapsed)"
  done

  echo "ERROR: Scene $SCENE_ID timed out after 10 minutes"
  return 1
}
```

Generate all scenes (run sequentially to avoid API rate limits):
```bash
python3 - <<'PYEOF'
import json, subprocess, os

script = json.load(open(os.environ['VIDEO_OUTPUT_DIR'] + '/script.json'))
for scene in script['scenes']:
    subprocess.run([
        'bash', '-c',
        f'generate_scene {scene["id"]} "{scene["video_prompt"]}" {scene["duration_seconds"]}'
    ], check=True)
PYEOF
```

Verify all scenes downloaded:
```bash
python3 - <<'PYEOF'
import json, os, subprocess
script = json.load(open(os.environ['VIDEO_OUTPUT_DIR'] + '/script.json'))
missing = []
for scene in script['scenes']:
    path = f"{os.environ['VIDEO_OUTPUT_DIR']}/scene_{scene['id']:03d}.mp4"
    if not os.path.exists(path) or os.path.getsize(path) < 50000:
        missing.append(path)
if missing:
    print(f"GATE FAILED — missing scenes: {missing}")
    exit(1)
print(f"GATE PASSED — {len(script['scenes'])} scenes ready")
PYEOF
```

**GATE [HARD]**: All scene MP4s downloaded, each > 50KB.
If a scene fails: retry that scene up to 2 times with a slightly modified prompt (add "high quality" or rephrase). If still failing after 3 attempts, abort and report the error.

---

## Phase 4: Post-Production (ffmpeg)

**Goal**: Merge scenes + narration into a single polished video.

**Step 4a — Concatenate scenes:**
```bash
# Build ffmpeg concat list
python3 - <<'PYEOF'
import json, os
script = json.load(open(os.environ['VIDEO_OUTPUT_DIR'] + '/script.json'))
outdir = os.environ['VIDEO_OUTPUT_DIR']
with open(f'{outdir}/concat.txt', 'w') as f:
    for scene in script['scenes']:
        path = f"{outdir}/scene_{scene['id']:03d}.mp4"
        f.write(f"file '{path}'\n")
print("Concat list written")
PYEOF

ffmpeg -y -f concat -safe 0 \
  -i "$VIDEO_OUTPUT_DIR/concat.txt" \
  -c copy \
  "$VIDEO_OUTPUT_DIR/scenes_merged.mp4"
```

**Step 4b — Layer narration audio:**
```bash
ffmpeg -y \
  -i "$VIDEO_OUTPUT_DIR/scenes_merged.mp4" \
  -i "$VIDEO_OUTPUT_DIR/narration.mp3" \
  -c:v copy \
  -c:a aac -b:a 192k \
  -map 0:v:0 -map 1:a:0 \
  -shortest \
  "$VIDEO_OUTPUT_DIR/video_with_audio.mp4"
```

**Step 4c — Generate subtitle file:**
```bash
python3 - <<'PYEOF'
import json, os
script = json.load(open(os.environ['VIDEO_OUTPUT_DIR'] + '/script.json'))
outdir = os.environ['VIDEO_OUTPUT_DIR']

def seconds_to_srt(s):
    h, r = divmod(int(s), 3600)
    m, sec = divmod(r, 60)
    return f"{h:02d}:{m:02d}:{sec:02d},000"

srt_content = ""
current_time = 0
for i, scene in enumerate(script['scenes'], 1):
    start = current_time
    end = current_time + scene['duration_seconds']
    text = scene['narration_segment']
    srt_content += f"{i}\n{seconds_to_srt(start)} --> {seconds_to_srt(end)}\n{text}\n\n"
    current_time = end

with open(f'{outdir}/subtitles.srt', 'w') as f:
    f.write(srt_content)
print("Subtitles written")
PYEOF
```

**Step 4d — Burn subtitles and finalize:**
```bash
ffmpeg -y \
  -i "$VIDEO_OUTPUT_DIR/video_with_audio.mp4" \
  -vf "subtitles=$VIDEO_OUTPUT_DIR/subtitles.srt:force_style='FontSize=20,PrimaryColour=&HFFFFFF,OutlineColour=&H000000,Outline=2,Bold=1'" \
  -c:a copy \
  "$VIDEO_OUTPUT_DIR/final_video.mp4"
```

---

## Phase 5: Quality Gate [HARD GATE]

```bash
python3 - <<'PYEOF'
import subprocess, json, sys, os

video = os.environ['VIDEO_OUTPUT_DIR'] + '/final_video.mp4'

# ffprobe analysis
result = subprocess.run([
    'ffprobe', '-v', 'quiet', '-print_format', 'json',
    '-show_format', '-show_streams', video
], capture_output=True, text=True)

data = json.loads(result.stdout)
duration = float(data['format']['duration'])
size = int(data['format']['size'])

video_stream = next((s for s in data['streams'] if s['codec_type'] == 'video'), None)
audio_stream = next((s for s in data['streams'] if s['codec_type'] == 'audio'), None)

errors = []
if duration < 10:
    errors.append(f"Video too short: {duration:.1f}s (min 10s)")
if size < 1_000_000:
    errors.append(f"File too small: {size} bytes (min 1MB)")
if not video_stream:
    errors.append("No video stream found")
if not audio_stream:
    errors.append("No audio stream found")
if video_stream:
    width = int(video_stream.get('width', 0))
    height = int(video_stream.get('height', 0))
    if width < 720:
        errors.append(f"Resolution too low: {width}x{height}")

if errors:
    print("QUALITY GATE FAILED:")
    for e in errors: print(f"  - {e}")
    sys.exit(1)

print(f"QUALITY GATE PASSED")
print(f"  Duration: {duration:.1f}s")
print(f"  Size: {size/1_000_000:.1f} MB")
print(f"  Resolution: {video_stream['width']}x{video_stream['height']}")
print(f"  Codec: {video_stream['codec_name']}")
PYEOF
```

If gate fails: identify the specific failure and re-run the relevant post-production step. Max 3 retries. If still failing after 3 retries, abort and report the error clearly.

---

## Phase 6: Generate Thumbnail (Imagen 4)

```bash
THUMBNAIL_PROMPT=$(python3 -c "import json,os; d=json.load(open(os.environ['VIDEO_OUTPUT_DIR']+'/script.json')); print(d['thumbnail_prompt'])")

curl -s -X POST \
  "https://generativelanguage.googleapis.com/v1beta/models/imagen-4.0-generate-001:predict" \
  -H "x-goog-api-key: $GEMINI_API_KEY" \
  -H "Content-Type: application/json" \
  -d "{
    \"instances\": [{\"prompt\": $(echo "$THUMBNAIL_PROMPT" | python3 -c "import json,sys; print(json.dumps(sys.stdin.read().strip()))")}],
    \"parameters\": {
      \"sampleCount\": 1,
      \"aspectRatio\": \"16:9\",
      \"imageSize\": \"1920x1080\"
    }
  }" | python3 - <<'PYEOF'
import json, sys, base64, os
data = json.load(sys.stdin)
img_b64 = data['predictions'][0]['bytesBase64Encoded']
out = os.environ['VIDEO_OUTPUT_DIR'] + '/thumbnail.jpg'
with open(out, 'wb') as f:
    f.write(base64.b64decode(img_b64))
print(f"Thumbnail saved: {out}")
PYEOF
```

---

## Phase 7: Upload & Distribution

### 7a — YouTube Upload

**Get access token first:**
```bash
YOUTUBE_ACCESS_TOKEN=$(curl -s -X POST "https://oauth2.googleapis.com/token" \
  -d "client_id=$YOUTUBE_CLIENT_ID" \
  -d "client_secret=$YOUTUBE_CLIENT_SECRET" \
  -d "refresh_token=$YOUTUBE_REFRESH_TOKEN" \
  -d "grant_type=refresh_token" \
  | python3 -c "import json,sys; print(json.loads(sys.stdin.read())['access_token'])")
```

**Upload the video:**
```bash
TITLE=$(python3 -c "import json,os; print(json.load(open(os.environ['VIDEO_OUTPUT_DIR']+'/script.json'))['title'])")
DESCRIPTION=$(python3 -c "import json,os; print(json.load(open(os.environ['VIDEO_OUTPUT_DIR']+'/script.json'))['description'])")
TAGS=$(python3 -c "import json,os; d=json.load(open(os.environ['VIDEO_OUTPUT_DIR']+'/script.json')); print(','.join(d['tags']))")

# Step 1: Initialize upload
UPLOAD_URL=$(curl -s -X POST \
  "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status" \
  -H "Authorization: Bearer $YOUTUBE_ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -D - \
  -d "{
    \"snippet\": {
      \"title\": $(echo "$TITLE" | python3 -c "import json,sys; print(json.dumps(sys.stdin.read().strip()))"),
      \"description\": $(echo "$DESCRIPTION" | python3 -c "import json,sys; print(json.dumps(sys.stdin.read().strip()))"),
      \"tags\": [$(echo "$TAGS" | python3 -c "import sys; tags=sys.stdin.read().strip().split(','); print(','.join(json.dumps(t) for t in tags))" 2>/dev/null || echo '"video","ai"')],
      \"categoryId\": \"28\"
    },
    \"status\": {
      \"privacyStatus\": \"public\"
    }
  }" | grep -i "Location:" | tr -d '\r' | awk '{print $2}')

# Step 2: Upload video bytes
VIDEO_SIZE=$(wc -c < "$VIDEO_OUTPUT_DIR/final_video.mp4")
YOUTUBE_VIDEO_ID=$(curl -s -X PUT "$UPLOAD_URL" \
  -H "Authorization: Bearer $YOUTUBE_ACCESS_TOKEN" \
  -H "Content-Type: video/mp4" \
  -H "Content-Length: $VIDEO_SIZE" \
  --data-binary "@$VIDEO_OUTPUT_DIR/final_video.mp4" \
  | python3 -c "import json,sys; d=json.loads(sys.stdin.read()); print(d.get('id','UPLOAD_FAILED'))")

echo "YouTube upload complete: https://youtube.com/watch?v=$YOUTUBE_VIDEO_ID"
echo "$YOUTUBE_VIDEO_ID" > "$VIDEO_OUTPUT_DIR/youtube_video_id.txt"
```

**Upload thumbnail:**
```bash
curl -s -X POST \
  "https://www.googleapis.com/upload/youtube/v3/thumbnails/set?videoId=$YOUTUBE_VIDEO_ID" \
  -H "Authorization: Bearer $YOUTUBE_ACCESS_TOKEN" \
  -H "Content-Type: image/jpeg" \
  --data-binary "@$VIDEO_OUTPUT_DIR/thumbnail.jpg"
```

### 7b — TikTok Upload (if TIKTOK_ACCESS_TOKEN set)

```bash
if [ -n "$TIKTOK_ACCESS_TOKEN" ]; then
  # Initialize upload
  TIKTOK_INIT=$(curl -s -X POST \
    "https://open.tiktokapis.com/v2/post/publish/video/init/" \
    -H "Authorization: Bearer $TIKTOK_ACCESS_TOKEN" \
    -H "Content-Type: application/json; charset=UTF-8" \
    -d "{
      \"post_info\": {
        \"title\": \"$(python3 -c "import json,os; print(json.load(open(os.environ['VIDEO_OUTPUT_DIR']+'/script.json'))['title'][:150])")\",
        \"privacy_level\": \"SELF_ONLY\",
        \"disable_duet\": false,
        \"disable_comment\": false,
        \"disable_stitch\": false
      },
      \"source_info\": {
        \"source\": \"FILE_UPLOAD\",
        \"video_size\": $(wc -c < "$VIDEO_OUTPUT_DIR/final_video.mp4"),
        \"chunk_size\": $(wc -c < "$VIDEO_OUTPUT_DIR/final_video.mp4"),
        \"total_chunk_count\": 1
      }
    }")

  UPLOAD_URL=$(echo "$TIKTOK_INIT" | python3 -c "import json,sys; d=json.loads(sys.stdin.read()); print(d['data']['upload_url'])")
  PUBLISH_ID=$(echo "$TIKTOK_INIT" | python3 -c "import json,sys; d=json.loads(sys.stdin.read()); print(d['data']['publish_id'])")

  # Upload video
  curl -s -X PUT "$UPLOAD_URL" \
    -H "Content-Type: video/mp4" \
    -H "Content-Range: bytes 0-$(($(wc -c < "$VIDEO_OUTPUT_DIR/final_video.mp4")-1))/$(wc -c < "$VIDEO_OUTPUT_DIR/final_video.mp4")" \
    --data-binary "@$VIDEO_OUTPUT_DIR/final_video.mp4"

  echo "TikTok publish ID: $PUBLISH_ID"
else
  echo "TIKTOK_ACCESS_TOKEN not set — skipping TikTok upload"
fi
```

### 7c — Cost Tracking Report

```bash
python3 - <<'PYEOF'
import json, os

script = json.load(open(os.environ['VIDEO_OUTPUT_DIR'] + '/script.json'))
scene_count = len(script['scenes'])
narration_chars = len(script['narration'])

# Estimated costs (adjust based on actual API pricing)
elevenlabs_cost = (narration_chars / 1000) * 0.30  # ~$0.30 per 1K chars
veo2_cost = scene_count * 0.50                      # ~$0.50 per scene (estimate)
imagen_cost = 0.04                                   # ~$0.04 per image
claude_cost = 0.08                                   # script generation estimate

total_cost = elevenlabs_cost + veo2_cost + imagen_cost + claude_cost

youtube_id = ""
try:
    youtube_id = open(os.environ['VIDEO_OUTPUT_DIR'] + '/youtube_video_id.txt').read().strip()
except:
    pass

report = {
    "video_title": script['title'],
    "youtube_url": f"https://youtube.com/watch?v={youtube_id}" if youtube_id else "N/A",
    "cost_breakdown": {
        "claude_script": f"${claude_cost:.2f}",
        "elevenlabs_tts": f"${elevenlabs_cost:.2f}",
        "veo2_video": f"${veo2_cost:.2f}",
        "imagen_thumbnail": f"${imagen_cost:.2f}",
        "total": f"${total_cost:.2f}"
    },
    "scenes_generated": scene_count,
    "narration_length_chars": narration_chars
}

print(json.dumps(report, indent=2))
with open(os.environ['VIDEO_OUTPUT_DIR'] + '/cost_report.json', 'w') as f:
    json.dump(report, f, indent=2)
PYEOF
```

---

## Error Handling Rules

1. **Phase 1 (Script)**: If JSON is invalid, regenerate. Max 3 attempts.
2. **Phase 2 (Voice)**: If ElevenLabs returns error, check API key validity with a test call. If text > 5000 chars, split into segments and concatenate MP3s with ffmpeg.
3. **Phase 3 (Video)**: If a scene fails to generate, retry with simplified prompt (remove unusual requests, add "simple" to prompt). Max 3 retries per scene.
4. **Phase 4 (Post-prod)**: If ffmpeg fails, print the exact command and error. Fix the specific issue (codec mismatch, missing file, etc.).
5. **Phase 5 (QA)**: Do NOT proceed to upload if quality gate fails.
6. **Phase 7 (Upload)**: If YouTube upload fails, save video locally and print the upload command for manual retry. Do NOT crash — log the error and continue to other platforms.

## Output on Completion

Print a final summary:
```
=== AI VIDEO CREATOR — COMPLETE ===
Title: [title]
YouTube: https://youtube.com/watch?v=[id]
TikTok: [status]
Cost: $[total]
Files: $VIDEO_OUTPUT_DIR/
===================================
```

## Connecting to android-app-builder

When called as a follow-up to a successful app build, use this template for the topic:
```
Create a 60-second app trailer for [APP_NAME]: [APP_DESCRIPTION].
Show: what the app does, key features, call to action to download.
Style: upbeat, professional, tech aesthetic.
```
