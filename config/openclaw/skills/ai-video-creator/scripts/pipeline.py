#!/usr/bin/env python3
"""
Dejima AI Video Creator — Full Autonomous Pipeline
Phases: Script → Voice → Video → Post-prod → QA → Thumbnail → YouTube upload
"""

import json, os, sys, time, subprocess, base64, urllib.parse, random
import requests
from google import genai
from google.genai import types as gtypes

# ── Config ──────────────────────────────────────────────────────────────────
TOPIC         = os.environ.get("TOPIC", "Golf Deals app — finds the best golf balls for your criteria")
# Optional detailed app brief — describe target user, key features, competitors, USP, real numbers
APP_BRIEF     = os.environ.get("APP_BRIEF", "")
OUTDIR        = os.environ.get("VIDEO_OUTPUT_DIR", "/tmp/dejima-videos/run")
FORMAT        = os.environ.get("FORMAT", "landscape")   # landscape (16:9) | portrait (9:16)
GEMINI_KEY    = os.environ.get("GEMINI_API_KEY", "")
GCP_PROJECT   = os.environ.get("GCP_PROJECT", "project-2c418787-8ea4-496d-a91")
GCP_LOCATION  = os.environ.get("GCP_LOCATION", "us-central1")
USE_VERTEX    = os.environ.get("USE_VERTEX", "1") == "1"  # default: Vertex AI
EL_KEY        = os.environ.get("ELEVENLABS_API_KEY", "")
EL_VOICE      = os.environ.get("ELEVENLABS_VOICE_ID", "")   # blank = random from pool
VOICE_STYLE   = os.environ.get("VOICE_STYLE", "random")     # "male" | "female" | "random"
YT_CLIENT_ID  = os.environ.get("YOUTUBE_CLIENT_ID", "")
YT_SECRET     = os.environ.get("YOUTUBE_CLIENT_SECRET", "")
YT_REFRESH    = os.environ.get("YOUTUBE_REFRESH_TOKEN", "")
TT_TOKEN      = os.environ.get("TIKTOK_ACCESS_TOKEN", "")
IG_USER_ID    = os.environ.get("INSTAGRAM_USER_ID", "")
IG_TOKEN      = os.environ.get("INSTAGRAM_ACCESS_TOKEN", "")
GEMINI_BASE   = "https://generativelanguage.googleapis.com/v1beta"

# Derive app name from topic (before "—" or "–")
APP_NAME = TOPIC.split("—")[0].split("–")[0].split("-")[0].strip()

# ── Voice pool (curated ElevenLabs pre-made voices for app ads) ───────────────
# Each voice tested for clarity, authority, and natural pacing on short-form content
VOICE_POOL = {
    "male": [
        "pNInz6obpgDQGcFmaJgB",  # Adam   — deep, authoritative
        "TxGEqnHWrfWFTfGW9XjX",  # Josh   — warm, conversational
        "ErXwobaYiN019PkySvjV",  # Antoni — natural, relatable
        "VR6AewLTigWG4xSOukaG",  # Arnold — confident, strong
        "ODq5zmih8GrVes37Dy39",  # Patrick — grounded, trustworthy
    ],
    "female": [
        "21m00Tcm4TlvDq8ikWAM",  # Rachel   — professional, clean
        "EXAVITQu4vr4xnSDxMaL",  # Bella    — warm, approachable
        "AZnzlk1XvdvUeBnXmlld",  # Domi     — energetic, direct
        "MF3mGyEYCl7XYWbV9V6O",  # Elli     — conversational, genuine
        "ThT5KcBeYPX3keUQqHPh",  # Dorothy  — upbeat, friendly
    ],
}

IS_SHORT      = FORMAT == "portrait"
ASPECT_RATIO  = "9:16" if IS_SHORT else "16:9"
# Shorts: 15s, ~40 words narration, 2 scenes. Landscape: 45s, ~130 words, 3 scenes.
SCENE_COUNT   = 2 if IS_SHORT else 3
WORD_TARGET   = "35-45 words (very tight, every word earns its place)" if IS_SHORT else "130-150 words, natural speaking pace"
DURATION_NOTE = "15-second short-form for TikTok/Reels/YouTube Shorts" if IS_SHORT else "45-second promo video for YouTube and paid ads"
YT_CATEGORY   = "22"  # People & Blogs (good for Shorts) vs 17 Sports

os.makedirs(OUTDIR, exist_ok=True)

# ── Google GenAI client ───────────────────────────────────────────────────────
if USE_VERTEX:
    gclient = genai.Client(vertexai=True, project=GCP_PROJECT, location=GCP_LOCATION)
else:
    gclient = genai.Client(api_key=GEMINI_KEY)

# ── Helpers ──────────────────────────────────────────────────────────────────
def ok(msg):  print(f"\033[32m✓ {msg}\033[0m")
def err(msg): print(f"\033[31m✗ {msg}\033[0m"); sys.exit(1)
def info(msg):print(f"\033[34m→ {msg}\033[0m")
def warn(msg):print(f"\033[33m⚠ {msg}\033[0m")
def header(n, title): print(f"\n\033[1m[ Phase {n} ] {title}\033[0m")

def gemini_post(path, body):
    url = f"{GEMINI_BASE}/{path}?key={GEMINI_KEY}"
    r = requests.post(url, json=body)
    r.raise_for_status()
    return r.json()

def gemini_get(path):
    url = f"{GEMINI_BASE}/{path}?key={GEMINI_KEY}"
    r = requests.get(url)
    r.raise_for_status()
    return r.json()

def checkpoint_exists(path):
    return os.path.exists(path) and os.path.getsize(path) > 1000

def get_access_token():
    r = subprocess.run(["gcloud", "auth", "application-default", "print-access-token"],
                       capture_output=True, text=True)
    return r.stdout.strip()

def vertex_post(path, body, token):
    r = requests.post(f"https://{GCP_LOCATION}-aiplatform.googleapis.com/v1/{path}",
                      json=body, headers={"Authorization": f"Bearer {token}", "Content-Type": "application/json"})
    r.raise_for_status()
    return r.json()

def vertex_get(path, token):
    r = requests.get(f"https://{GCP_LOCATION}-aiplatform.googleapis.com/v1/{path}",
                     headers={"Authorization": f"Bearer {token}"})
    if r.status_code == 401:
        token = get_access_token()
        r = requests.get(f"https://{GCP_LOCATION}-aiplatform.googleapis.com/v1/{path}",
                         headers={"Authorization": f"Bearer {token}"})
    r.raise_for_status()
    return r.json(), token

def ffprobe_info(path):
    r = subprocess.run(["ffprobe","-v","quiet","-print_format","json","-show_format","-show_streams",path],
                       capture_output=True, text=True)
    return json.loads(r.stdout)

# ── Banner ───────────────────────────────────────────────────────────────────
fmt_label = "📱 PORTRAIT — YouTube Shorts / TikTok / Reels (9:16, 15s)" if IS_SHORT else "🖥  LANDSCAPE — YouTube / Paid Ads (16:9, 45s)"
print("\n\033[1m" + "="*54)
print("   DEJIMA AI VIDEO CREATOR")
print("="*54 + "\033[0m")
print(f"App:    {TOPIC}")
print(f"Format: {fmt_label}")
print(f"Output: {OUTDIR}\n")

# ── Phase 0: Deep App Analysis (optional) ────────────────────────────────────
APP_SOURCE_DIR = os.environ.get("APP_SOURCE_DIR", "")
BRIEF_PATH = f"{OUTDIR}/app_brief.txt"

if APP_SOURCE_DIR and os.path.isdir(APP_SOURCE_DIR):
    header("0/7", f"App Analysis — {APP_SOURCE_DIR}")
    if os.path.exists(BRIEF_PATH) and os.path.getsize(BRIEF_PATH) > 200:
        ok("App brief already generated — loading checkpoint")
        with open(BRIEF_PATH) as f:
            APP_BRIEF = f.read()
    else:
        info("Reading app source to generate creative brief...")

        # Collect relevant files: README, manifest, main source, key classes
        INCLUDE_EXTENSIONS = {".md", ".txt", ".py", ".js", ".ts", ".kt", ".java",
                               ".swift", ".dart", ".json", ".yaml", ".yml", ".toml"}
        SKIP_DIRS = {"node_modules", ".git", "__pycache__", "build", "dist",
                     ".gradle", ".idea", "venv", ".env", "coverage", "test", "tests", "__tests__"}
        PRIORITY_FILES = {"readme.md", "readme.txt", "readme", "package.json",
                          "androidmanifest.xml", "info.plist", "pubspec.yaml",
                          "requirements.txt", "pyproject.toml", "main.py",
                          "main.kt", "mainactivity.kt", "app.py", "index.js",
                          "index.ts", "app.js", "app.ts"}

        collected = []   # (priority, path, content)
        total_chars = 0
        MAX_CHARS = 40_000  # stay well within context limits

        for root, dirs, files in os.walk(APP_SOURCE_DIR):
            # Prune skip dirs in-place
            dirs[:] = [d for d in dirs if d.lower() not in SKIP_DIRS and not d.startswith(".")]
            rel_root = os.path.relpath(root, APP_SOURCE_DIR)
            depth = 0 if rel_root == "." else rel_root.count(os.sep) + 1

            for fname in files:
                fpath = os.path.join(root, fname)
                ext = os.path.splitext(fname)[1].lower()
                if ext not in INCLUDE_EXTENSIONS:
                    continue
                try:
                    size = os.path.getsize(fpath)
                    if size > 50_000:  # skip very large files
                        continue
                    with open(fpath, "r", errors="ignore") as f:
                        content = f.read(8000)  # cap per file
                    rel_path = os.path.relpath(fpath, APP_SOURCE_DIR)
                    is_priority = fname.lower() in PRIORITY_FILES
                    priority = (0 if is_priority else 1, depth, fname)
                    collected.append((priority, rel_path, content))
                except Exception:
                    pass

        # Sort: priority files first, then by depth (shallow first)
        collected.sort(key=lambda x: x[0])

        # Build context string up to MAX_CHARS
        context_parts = []
        for _, rel_path, content in collected:
            snippet = f"### {rel_path}\n{content}\n"
            if total_chars + len(snippet) > MAX_CHARS:
                break
            context_parts.append(snippet)
            total_chars += len(snippet)

        if not context_parts:
            warn("No readable source files found — skipping app analysis")
        else:
            info(f"Analyzing {len(context_parts)} files ({total_chars//1000}KB)...")
            analysis_prompt = f"""You are a product strategist and performance marketer analyzing a mobile app's source code to write a creative brief for a short-form video ad.

Read the following source code and files from the app, then produce a creative brief that a copywriter can use to write a 15-second YouTube Short script.

APP NAME (from team): {APP_NAME}

SOURCE FILES:
{chr(10).join(context_parts)}

Write a CREATIVE BRIEF with these exact sections:
1. TARGET USER: Who is this app for? Be specific — age range, situation, what they do before downloading this app.
2. CORE PROBLEM: What frustration does this solve? State it the way the user would say it in their head.
3. KEY VALUE PROP: What is the ONE thing this app does that nothing else does? Be specific.
4. PROOF POINTS: Any specific numbers, metrics, or facts visible in the code (e.g., "saves X minutes", "X% cheaper", "connects to N retailers"). If not explicit, estimate honestly.
5. COMPETITORS / ALTERNATIVES: What do people use instead? Why do those fail?
6. EMOTIONAL HOOK: What feeling does the user have before using this app? What feeling after?
7. CTA: What should the user do? What's the lowest friction entry point?

Be specific and concrete. If the code reveals technical details that translate to user benefits, extract them. Do NOT be generic. Do NOT use buzzwords.
Write only the brief, nothing else."""

            resp = gemini_post("models/gemini-2.5-flash:generateContent", {
                "contents": [{"parts": [{"text": analysis_prompt}]}],
                "generationConfig": {"temperature": 0.3, "maxOutputTokens": 8192}
            })
            cand = resp["candidates"][0]
            APP_BRIEF = cand["content"]["parts"][0]["text"].strip()
            finish = cand.get("finishReason", "UNKNOWN")
            if finish not in ("STOP", "MAX_TOKENS"):
                warn(f"Brief generation ended with finishReason={finish}")
            with open(BRIEF_PATH, "w") as f:
                f.write(APP_BRIEF)
            ok(f"Creative brief generated ({len(APP_BRIEF)} chars)")
            info(f"Brief preview: {APP_BRIEF[:200]}...")

elif APP_BRIEF:
    info(f"Using provided APP_BRIEF ({len(APP_BRIEF)} chars)")
else:
    info("No APP_SOURCE_DIR or APP_BRIEF — using TOPIC only for script generation")

# ── Phase 1: Script Generation ───────────────────────────────────────────────
header("1/7", "Script Generation")
SCRIPT_PATH = f"{OUTDIR}/script.json"
if checkpoint_exists(SCRIPT_PATH):
    ok("Script already generated — loading checkpoint")
    with open(SCRIPT_PATH) as f:
        script = json.load(f)
else:
    info("Generating promo script with Gemini 2.5 Flash...")
    scenes_template = "\n".join([
        f"""    {{
      "id": {i},
      "narration_segment": "Words spoken during scene {i}",
      "video_prompt": "Cinematic visual for scene {i}. NO app UI screens, NO phones with visible text, NO logos, NO faces in close-up. Describe: camera angle, subject, environment, lighting, motion. End with: cinematic 4K professional"
    }}""" for i in range(1, SCENE_COUNT + 1)
    ])
    brief_section = f"\nDETAILED APP BRIEF:\n{APP_BRIEF}\n" if APP_BRIEF else ""

    short_rules = """
FORMAT: 15-second YouTube Short / TikTok / Reel
STRUCTURE (non-negotiable):
  0.0–1.5s  HOOK — one sentence that IS the target user's exact unspoken thought
  1.5–5s    PROBLEM — one sharp sentence that twists the knife
  5–12s     SOLUTION — specific outcome, one concrete number or fact if possible
  12–15s    CTA — direct action, low friction ("Free" or "30 seconds" if honest)

Total narration: 35-45 words MAXIMUM. Cut every word that doesn't earn its place.
Title must end with #Shorts
""" if IS_SHORT else """
FORMAT: 45-second YouTube promo / paid ad
STRUCTURE:
  0–5s    HOOK — open on the user's problem, not the product
  5–20s   PROBLEM — specific pain, relatable situation
  20–38s  SOLUTION — demonstrate value with specifics
  38–45s  CTA + social proof if available

Total narration: 130-150 words, natural speaking pace.
"""
    prompt_text = f"""You are a performance creative director who has written short-form video ads that generated real revenue for mobile apps. You've worked with direct-response teams that live and die by ROAS, CPE, and day-7 retention — not vanity metrics.

Your job: write a {DURATION_NOTE} script for this app.

APP: {TOPIC}{brief_section}
{short_rules}
BEFORE WRITING, answer these internally (do not include in output):
1. Who is this person watching at 11pm on their phone? What do they do, what frustrates them?
2. What have they tried before that didn't work? Why did it fail?
3. What is the ONE specific thing this app does that nothing else does?
4. What does their life look like 24 hours after downloading? What's measurably different?
5. What would make THIS specific person stop scrolling in under 2 seconds?

WRITING RULES — every single one is mandatory:
- First word is NEVER the app name, never "Are you tired", never "Introducing"
- Open with the user's internal thought or a pattern-interrupt fact, NOT a question
- Use "you" — talk to one specific person, not an audience
- Specificity = credibility: "saves 23 minutes per round" beats "saves time"
- Show the result/feeling, not the feature: "you stop second-guessing" not "AI-powered recommendations"
- Every sentence must either hook deeper OR move toward the CTA — no filler
- BANNED words: revolutionary, game-changing, seamless, unlock, next-level, powerful, smart, intuitive, amazing, incredible, innovative, disruptive, cutting-edge, state-of-the-art, leverage, synergy
- CTA has a verb + the lowest real friction barrier. Never "Learn more."

VIDEO PROMPT RULES (Veo AI will reject scenes that break these — follow exactly):
- NEVER name the app, any brand, or a logo
- NEVER show a phone with visible UI, text, or app screens
- NEVER describe a human face in close-up
- DO describe: environments, objects, hands, body language, silhouettes, abstract motion, physical products, nature
- Good: "Close-up of a weathered hand sorting through golf balls in a canvas bag pocket, early morning mist, shallow depth of field, cinematic 4K professional"
- Bad: "Person using the Golf Deals app on their phone to compare golf ball prices"
- End every video_prompt with: cinematic 4K professional

Return ONLY valid JSON — no markdown, no backticks, no explanation:
{{
  "title": "YouTube title under 70 chars. Specific benefit or question. Not clickbait, genuinely honest.",
  "description": "YouTube description 200-250 chars. Opens with the hook, names the specific benefit, ends with CTA.",
  "tags": ["tag1", "tag2", "tag3", "tag4", "tag5"],
  "narration": "Complete spoken narration. {WORD_TARGET}. Every word earns its place.",
  "scenes": [
{scenes_template}
  ],
  "thumbnail_prompt": "Bold YouTube thumbnail. Deep, rich background color (not white). Specific physical subject directly related to the app's core value. Strong contrast. Room for text overlay on left third. Photorealistic, dramatic lighting. Absolutely no text in the image itself.",
  "music_prompt": "Instrumental background music for a mobile app ad. Under 30 words. Describe: mood, instruments, tempo, energy level. Must feel professional and match the emotional arc of the script.",
  "cta_text": "Short CTA under 5 words shown at end of video. Specific action word. e.g. 'Download Free', 'Try It Now', 'Get Yours Free'"
}}"""
    resp = gemini_post("models/gemini-2.5-flash:generateContent", {
        "contents": [{"parts": [{"text": prompt_text}]}],
        "generationConfig": {"temperature": 0.7, "maxOutputTokens": 8192}
    })
    raw = resp["candidates"][0]["content"]["parts"][0]["text"].strip()
    # Strip markdown code fences if Gemini added them
    if raw.startswith("```"):
        lines = raw.split("\n")
        raw = "\n".join(lines[1:] if lines[-1] != "```" else lines[1:-1])
    script = json.loads(raw)
    # Enforce scene count — Gemini sometimes ignores the template
    if len(script["scenes"]) > SCENE_COUNT:
        warn(f"Gemini generated {len(script['scenes'])} scenes, trimming to {SCENE_COUNT}")
        script["scenes"] = script["scenes"][:SCENE_COUNT]
    elif len(script["scenes"]) < SCENE_COUNT:
        warn(f"Gemini generated only {len(script['scenes'])} scenes (expected {SCENE_COUNT}) — continuing anyway")
    # Re-number scene IDs sequentially
    for i, s in enumerate(script["scenes"]):
        s["id"] = i + 1
    with open(SCRIPT_PATH, "w") as f:
        json.dump(script, f, indent=2)

ok(f"Script: \"{script['title']}\"")
info(f"Scenes: {len(script['scenes'])} | Narration: {len(script['narration'])} chars")

# ── Phase 2: Voice Generation ────────────────────────────────────────────────
header("2/7", "Voice Generation (ElevenLabs)")
NARRATION_PATH = f"{OUTDIR}/narration.mp3"
VOICE_ID_PATH  = f"{OUTDIR}/voice_id.txt"

# Resolve which voice to use (checkpoint: reuse same voice on re-run)
if os.path.exists(VOICE_ID_PATH):
    with open(VOICE_ID_PATH) as f:
        chosen_voice = f.read().strip()
    info(f"Voice (from checkpoint): {chosen_voice}")
elif EL_VOICE:
    chosen_voice = EL_VOICE
    info(f"Voice (from ELEVENLABS_VOICE_ID env): {chosen_voice}")
else:
    # Pick randomly from curated pool
    if VOICE_STYLE == "male":
        pool = VOICE_POOL["male"]
    elif VOICE_STYLE == "female":
        pool = VOICE_POOL["female"]
    else:
        pool = VOICE_POOL["male"] + VOICE_POOL["female"]
    chosen_voice = random.choice(pool)
    gender_label = "male" if chosen_voice in VOICE_POOL["male"] else "female"
    info(f"Voice (random {gender_label}): {chosen_voice}")

# Save voice choice for this run
with open(VOICE_ID_PATH, "w") as f:
    f.write(chosen_voice)

if checkpoint_exists(NARRATION_PATH):
    ok("Narration already generated — loading checkpoint")
else:
    info(f"Generating narration ({len(script['narration'])} chars)...")
    tts_r = requests.post(
        f"https://api.elevenlabs.io/v1/text-to-speech/{chosen_voice}",
        headers={"xi-api-key": EL_KEY, "Content-Type": "application/json"},
        json={"text": script["narration"], "model_id": "eleven_flash_v2_5",
              "voice_settings": {"stability": 0.5, "similarity_boost": 0.75, "style": 0.3, "use_speaker_boost": True}}
    )
    if tts_r.status_code != 200:
        err(f"ElevenLabs failed ({tts_r.status_code}): {tts_r.text[:200]}")
    with open(NARRATION_PATH, "wb") as f:
        f.write(tts_r.content)
info_d = ffprobe_info(NARRATION_PATH)
dur = float(info_d["format"]["duration"])
ok(f"Narration: {dur:.1f}s audio")

# ── Phase 2.5: Background Music (Lyria 2) ─────────────────────────────────────
header("2.5/7", "Background Music (Lyria 2)")
MUSIC_PATH = f"{OUTDIR}/music.wav"
MUSIC_MIXED_PATH = f"{OUTDIR}/narration_mixed.mp3"

if checkpoint_exists(MUSIC_PATH):
    ok("Music already generated — loading checkpoint")
else:
    music_prompt = script.get("music_prompt",
        "Upbeat, confident, clean instrumental background music for a mobile app advertisement. "
        "Modern minimal piano and light percussion. Professional tone, no vocals, 120 BPM.")
    # Strip any video quality descriptors that Gemini might have appended to the music prompt
    for tail in ["cinematic 4K professional", "4K professional", "cinematic", "professional"]:
        if music_prompt.endswith(tail):
            music_prompt = music_prompt[:-len(tail)].rstrip(". ,")
    info(f"Generating music: {music_prompt[:80]}...")
    try:
        token = get_access_token() if USE_VERTEX else None
        # Lyria 2 Vertex AI: instances use "prompt" key, parameters use camelCase sampleCount
        music_body = {
            "instances": [{"prompt": music_prompt}],
            "parameters": {"sampleCount": 1}
        }
        if USE_VERTEX:
            music_resp = vertex_post(
                f"projects/{GCP_PROJECT}/locations/{GCP_LOCATION}/publishers/google/models/lyria-002:predict",
                music_body, token)
        else:
            music_resp = gemini_post("models/lyria-002:predict", music_body)
        pred = music_resp.get("predictions", [{}])[0]
        audio_b64 = (pred.get("audioContent") or pred.get("audio_content") or
                     pred.get("bytesBase64Encoded") or pred.get("encodedAudio"))
        if not audio_b64:
            raise Exception(f"Unknown response keys: {list(pred.keys())} — full: {str(pred)[:200]}")
        with open(MUSIC_PATH, "wb") as f:
            f.write(base64.b64decode(audio_b64))
        ok(f"Music generated: {os.path.getsize(MUSIC_PATH)//1000}KB WAV")
    except Exception as e:
        warn(f"Lyria music failed ({str(e)[:120]}) — continuing without music")
        MUSIC_PATH = None

# ── Phase 3: Video Scene Generation (Veo 3 via Vertex AI REST) ────────────────
header("3/7", "Video Generation (Veo 3.1 via Vertex AI)")
info(f"Generating {len(script['scenes'])} scenes...")

def generate_scene(sid, vprompt, out_f, max_retries=3):
    for attempt in range(max_retries):
        try:
            token = get_access_token()
            info(f"Scene {sid}: submitting to Veo 3.1 Vertex AI (attempt {attempt+1})...")

            # Submit via Vertex AI REST
            endpoint = f"projects/{GCP_PROJECT}/locations/{GCP_LOCATION}/publishers/google/models/veo-3.1-generate-preview:predictLongRunning"
            body = {
                "instances": [{"prompt": vprompt}],
                "parameters": {"aspectRatio": ASPECT_RATIO, "sampleCount": 1, "durationSeconds": 8}
            }
            op = vertex_post(endpoint, body, token)
            op_name = op.get("name", "")
            info(f"Scene {sid}: op_name = {op_name}")
            if not op_name:
                warn(f"Scene {sid}: full response: {json.dumps(op)[:300]}. Retrying...")
                continue
            info(f"Scene {sid}: operation started")

            # Poll via fetchPredictOperation (correct endpoint for predictLongRunning)
            fetch_endpoint = f"projects/{GCP_PROJECT}/locations/{GCP_LOCATION}/publishers/google/models/veo-3.1-generate-preview:fetchPredictOperation"
            done_status = None
            for tick_i in range(60):
                time.sleep(10)
                tick = (tick_i + 1) * 10
                status = vertex_post(fetch_endpoint, {"operationName": op_name}, token)
                print(f"  Scene {sid}: generating... ({tick}s)")
                if status.get("done"):
                    done_status = status
                    break

            if not done_status:
                warn(f"Scene {sid}: timed out. Retrying..."); continue
            if "error" in done_status:
                warn(f"Scene {sid}: error: {done_status['error']}. Retrying..."); continue

            # Extract video — Vertex AI returns bytesBase64Encoded directly
            response = done_status.get("response", {})
            video_b64 = None
            for v in response.get("videos", []):
                video_b64 = v.get("bytesBase64Encoded")
                if video_b64: break

            if video_b64:
                info(f"Scene {sid}: decoding video ({len(video_b64)//1000}KB b64)...")
                with open(out_f, "wb") as f:
                    f.write(base64.b64decode(video_b64))
            else:
                # Fallback: try URI-based download
                video_uri = None
                for sample in response.get("generatedSamples", []):
                    video_uri = sample.get("video", {}).get("uri") or sample.get("video", {}).get("gcsUri")
                    if video_uri: break
                if not video_uri:
                    warn(f"Scene {sid}: no video data in response: {json.dumps(response)[:200]}. Retrying...")
                    continue
                info(f"Scene {sid}: downloading from URI...")
                if video_uri.startswith("gs://"):
                    bucket, blob = video_uri[5:].split("/", 1)
                    video_uri = f"https://storage.googleapis.com/download/storage/v1/b/{bucket}/o/{urllib.parse.quote(blob, safe='')}?alt=media"
                token = get_access_token()
                dl_r = requests.get(video_uri, headers={"Authorization": f"Bearer {token}"}, stream=True)
                dl_r.raise_for_status()
                with open(out_f, "wb") as f:
                    for chunk in dl_r.iter_content(65536): f.write(chunk)

            size = os.path.getsize(out_f)
            MIN_SIZE = 500_000 if IS_SHORT else 1_500_000
            if size < MIN_SIZE:
                warn(f"Scene {sid}: black screen ({size//1000}KB). Retrying...")
                os.remove(out_f); continue

            ok(f"Scene {sid} (Veo 3.1 Vertex AI, {ASPECT_RATIO}): {size/1000:.0f}KB")
            return

        except Exception as e:
            e_str = str(e)
            if "429" in e_str or "RESOURCE_EXHAUSTED" in e_str:
                warn(f"Scene {sid}: quota hit — waiting 60s..."); time.sleep(60)
            else:
                warn(f"Scene {sid}: {e_str[:150]}. Retrying in 10s..."); time.sleep(10)

    err(f"Scene {sid} failed after {max_retries} attempts")

for i, scene in enumerate(script["scenes"]):
    sid   = scene["id"]
    out_f = f"{OUTDIR}/scene_{sid:03d}.mp4"
    if os.path.exists(out_f) and os.path.getsize(out_f) > 200_000:
        ok(f"Scene {sid} already generated — skipping"); continue
    if i > 0:
        info("Waiting 15s between scenes..."); time.sleep(15)
    generate_scene(sid, scene["video_prompt"], out_f)

# ── Phase 4: Post-Production (ffmpeg) ────────────────────────────────────────
header("4/7", "Post-Production (ffmpeg)")

# Concat list
concat_path = f"{OUTDIR}/concat.txt"
with open(concat_path, "w") as f:
    for s in script["scenes"]:
        f.write(f"file '{OUTDIR}/scene_{s['id']:03d}.mp4'\n")

info("Concatenating scenes...")
r = subprocess.run(["ffmpeg","-y","-loglevel","error","-f","concat","-safe","0",
                    "-i", concat_path, "-c","copy", f"{OUTDIR}/scenes_merged.mp4"])
if r.returncode != 0: err("Scene concatenation failed")
ok("Scenes concatenated")

info("Layering audio (narration + music)...")
has_music = MUSIC_PATH and os.path.exists(MUSIC_PATH) and os.path.getsize(MUSIC_PATH) > 1000
if has_music:
    # Mix: narration at full volume, music ducked to 12% underneath, fade out last 2s
    r = subprocess.run(["ffmpeg","-y","-loglevel","error",
        "-i", f"{OUTDIR}/scenes_merged.mp4",
        "-i", f"{OUTDIR}/narration.mp3",
        "-i", MUSIC_PATH,
        "-filter_complex",
        f"[1:a]volume=1.0[narr];[2:a]volume=0.12,atrim=0:{dur:.3f},afade=out:st={max(0,dur-2):.3f}:d=2[mus];[narr][mus]amix=inputs=2:duration=first[aout]",
        "-map","0:v:0","-map","[aout]","-c:v","copy","-c:a","aac","-b:a","192k","-shortest",
        f"{OUTDIR}/video_with_audio.mp4"])
else:
    r = subprocess.run(["ffmpeg","-y","-loglevel","error",
        "-i", f"{OUTDIR}/scenes_merged.mp4",
        "-i", f"{OUTDIR}/narration.mp3",
        "-c:v","copy","-c:a","aac","-b:a","192k",
        "-map","0:v:0","-map","1:a:0","-shortest",
        f"{OUTDIR}/video_with_audio.mp4"])
if r.returncode != 0: err("Audio merge failed")
ok(f"Audio merged {'(narration + music)' if has_music else '(narration only)'}")

# SRT subtitles
narr_dur = float(ffprobe_info(f"{OUTDIR}/narration.mp3")["format"]["duration"])
scenes   = script["scenes"]
per_scene = narr_dur / len(scenes)

def to_srt(s):
    h,rem = divmod(int(s), 3600); m,sec = divmod(rem, 60)
    return f"{h:02d}:{m:02d}:{sec:02d},000"

srt = ""
for i, s in enumerate(scenes):
    start, end = i*per_scene, (i+1)*per_scene
    # Handle Gemini typos: narration_segment / naration_segment / narration / text
    text = (s.get("narration_segment") or s.get("naration_segment") or
            s.get("narration") or s.get("text") or f"Scene {s.get('id',i+1)}")
    srt += f"{i+1}\n{to_srt(start)} --> {to_srt(end)}\n{text}\n\n"

srt_path = f"{OUTDIR}/subtitles.srt"
with open(srt_path, "w") as f:
    f.write(srt)

info("Burning subtitles + text overlays...")
cta = script.get("cta_text", "Download Free →").replace("'", "").replace(":", "\\:")
app_safe = APP_NAME.replace("'", "").replace(":", "\\:")
font_sz_sub  = 22 if IS_SHORT else 18
font_sz_app  = 38 if IS_SHORT else 30
font_sz_cta  = 32 if IS_SHORT else 26
# Text overlay: app name top-center for first 3s, CTA bottom-center last 3s
overlay_vf = (
    f"subtitles='{srt_path}':force_style='FontSize={font_sz_sub},PrimaryColour=&HFFFFFF,OutlineColour=&H000000,Outline=2,Bold=1',"
    f"drawtext=text='{app_safe}':fontsize={font_sz_app}:fontcolor=white:borderw=3:bordercolor=black@0.8"
    f":x=(w-text_w)/2:y=60:enable='between(t,0,3)',"
    f"drawtext=text='{cta}':fontsize={font_sz_cta}:fontcolor=0xC9A84C:borderw=3:bordercolor=black@0.8"
    f":x=(w-text_w)/2:y=h-90:enable='between(t,{max(0,narr_dur-3):.1f},{narr_dur:.1f})'"
)
r = subprocess.run(["ffmpeg","-y","-loglevel","error",
    "-i", f"{OUTDIR}/video_with_audio.mp4",
    "-vf", overlay_vf,
    "-c:a","copy", f"{OUTDIR}/final_video.mp4"])

if r.returncode == 0:
    ok("Subtitles + text overlays burned")
else:
    warn("Overlay failed — trying subtitles only...")
    r2 = subprocess.run(["ffmpeg","-y","-loglevel","error",
        "-i", f"{OUTDIR}/video_with_audio.mp4",
        "-vf", f"subtitles='{srt_path}':force_style='FontSize={font_sz_sub},PrimaryColour=&HFFFFFF,OutlineColour=&H000000,Outline=2,Bold=1'",
        "-c:a","copy", f"{OUTDIR}/final_video.mp4"])
    if r2.returncode != 0:
        warn("Subtitle burn also failed — using raw video")
        subprocess.run(["cp", f"{OUTDIR}/video_with_audio.mp4", f"{OUTDIR}/final_video.mp4"])

# ── Phase 5: Quality Gate ────────────────────────────────────────────────────
header("5/7", "Quality Gate")

data = ffprobe_info(f"{OUTDIR}/final_video.mp4")
dur  = float(data["format"]["duration"])
size = int(data["format"]["size"])
vs   = next((s for s in data["streams"] if s["codec_type"]=="video"), None)
as_  = next((s for s in data["streams"] if s["codec_type"]=="audio"), None)

errors = []
if dur  < 5:         errors.append(f"Too short: {dur:.1f}s")
if size < 500_000:   errors.append(f"Too small: {size} bytes")
if not vs:           errors.append("No video stream")
if not as_:          errors.append("No audio stream")

if errors:
    err("Quality gate FAILED: " + " | ".join(errors))

ok(f"{dur:.1f}s | {size/1_000_000:.1f}MB | {vs['width']}x{vs['height']} {vs['codec_name']}")

# ── Phase 6: Thumbnail (Imagen 4) ────────────────────────────────────────────
header("6/7", "Thumbnail Generation (Imagen 4)")
info("Generating thumbnail...")

try:
    thumb_resp = gclient.models.generate_images(
        model="imagen-4.0-generate-001",
        prompt=script["thumbnail_prompt"],
        config=gtypes.GenerateImagesConfig(number_of_images=1, aspect_ratio="16:9")
    )
    img_bytes = thumb_resp.generated_images[0].image.image_bytes
    with open(f"{OUTDIR}/thumbnail.jpg", "wb") as f:
        f.write(img_bytes)
    ok(f"Thumbnail (Imagen 4 via SDK): {len(img_bytes)//1000}KB")
    HAS_THUMB = True
except Exception as e:
    warn(f"SDK thumbnail failed ({e}), trying REST fallback...")
    thumb_rest = gemini_post("models/imagen-4.0-generate-001:predict", {
        "instances": [{"prompt": script["thumbnail_prompt"]}],
        "parameters": {"sampleCount": 1, "aspectRatio": "16:9"}
    })
    if "predictions" in thumb_rest:
        img_bytes = base64.b64decode(thumb_rest["predictions"][0]["bytesBase64Encoded"])
        with open(f"{OUTDIR}/thumbnail.jpg", "wb") as f:
            f.write(img_bytes)
        ok(f"Thumbnail (REST fallback): {len(img_bytes)//1000}KB")
        HAS_THUMB = True
    else:
        warn(f"Thumbnail failed: {str(thumb_rest)[:100]}")
        HAS_THUMB = False

# ── Phase 7: YouTube Upload ──────────────────────────────────────────────────
header("7/7", "YouTube Upload")

YOUTUBE_URL = None

if not all([YT_CLIENT_ID, YT_SECRET, YT_REFRESH]):
    warn("YouTube credentials missing — skipping upload")
else:
    info("Refreshing YouTube access token...")
    token_r = requests.post("https://oauth2.googleapis.com/token", data={
        "client_id": YT_CLIENT_ID, "client_secret": YT_SECRET,
        "refresh_token": YT_REFRESH, "grant_type": "refresh_token"
    })
    access_token = token_r.json().get("access_token")
    if not access_token:
        warn(f"Token refresh failed: {token_r.text[:200]}")
    else:
        ok("Access token obtained")
        info("Initializing YouTube resumable upload...")

        init_r = requests.post(
            "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=resumable&part=snippet,status",
            headers={"Authorization": f"Bearer {access_token}",
                     "Content-Type": "application/json",
                     "X-Upload-Content-Type": "video/mp4"},
            json={"snippet": {
                    "title": (script["title"].rstrip(".") + " #Shorts")[:100] if IS_SHORT else script["title"][:100],
                    "description": script["description"],
                    "tags": (["Shorts"] + script.get("tags", [])) if IS_SHORT else script.get("tags", []),
                    "categoryId": "17"},
                  "status": {"privacyStatus": "public"}}
        )
        upload_url = init_r.headers.get("Location")

        if not upload_url:
            warn(f"No upload URL returned: {init_r.text[:200]}")
        else:
            info("Uploading video to YouTube...")
            with open(f"{OUTDIR}/final_video.mp4", "rb") as f:
                up_r = requests.put(upload_url,
                    headers={"Authorization": f"Bearer {access_token}", "Content-Type": "video/mp4"},
                    data=f)
            yt_id = up_r.json().get("id")
            if yt_id:
                YOUTUBE_URL = f"https://youtube.com/watch?v={yt_id}"
                with open(f"{OUTDIR}/youtube_id.txt", "w") as f:
                    f.write(yt_id)
                ok(f"Uploaded: {YOUTUBE_URL}")
                if HAS_THUMB:
                    info("Setting thumbnail...")
                    with open(f"{OUTDIR}/thumbnail.jpg", "rb") as f:
                        th_r = requests.post(
                            f"https://www.googleapis.com/upload/youtube/v3/thumbnails/set?videoId={yt_id}",
                            headers={"Authorization": f"Bearer {access_token}", "Content-Type": "image/jpeg"},
                            data=f)
                    ok("Thumbnail set") if th_r.ok else warn(f"Thumbnail failed: {th_r.text[:100]}")
            else:
                warn(f"Upload response missing ID: {up_r.text[:200]}")

# ── TikTok Upload ─────────────────────────────────────────────────────────────
TIKTOK_URL = None
if not TT_TOKEN:
    info("TikTok: no TIKTOK_ACCESS_TOKEN in .env — skipping")
else:
    info("Uploading to TikTok...")
    try:
        video_size = os.path.getsize(f"{OUTDIR}/final_video.mp4")
        init_r = requests.post(
            "https://open.tiktokapis.com/v2/post/publish/inbox/video/init/",
            headers={"Authorization": f"Bearer {TT_TOKEN}", "Content-Type": "application/json; charset=UTF-8"},
            json={"source_info": {"source": "FILE_UPLOAD", "video_size": video_size,
                                   "chunk_size": video_size, "total_chunk_count": 1}}
        )
        init_data = init_r.json().get("data", {})
        publish_id = init_data.get("publish_id")
        upload_url = init_data.get("upload_url")
        if upload_url:
            with open(f"{OUTDIR}/final_video.mp4", "rb") as f:
                video_bytes = f.read()
            up_r = requests.put(upload_url,
                headers={"Content-Range": f"bytes 0-{video_size-1}/{video_size}",
                         "Content-Type": "video/mp4"},
                data=video_bytes)
            if up_r.ok:
                TIKTOK_URL = f"https://www.tiktok.com (publish_id: {publish_id})"
                ok(f"TikTok uploaded (PRIVATE — make public in TikTok app): {publish_id}")
                with open(f"{OUTDIR}/tiktok_id.txt", "w") as f: f.write(publish_id or "")
            else:
                warn(f"TikTok upload failed: {up_r.text[:150]}")
        else:
            warn(f"TikTok init failed: {init_r.text[:150]}")
    except Exception as e:
        warn(f"TikTok upload error: {str(e)[:100]}")

# ── Instagram Upload ──────────────────────────────────────────────────────────
INSTAGRAM_URL = None
if not IG_TOKEN or not IG_USER_ID:
    info("Instagram: no INSTAGRAM_USER_ID / INSTAGRAM_ACCESS_TOKEN in .env — skipping")
elif not YOUTUBE_URL:
    warn("Instagram: requires a public video URL — skipping (no YouTube URL available)")
else:
    info("Uploading to Instagram Reels...")
    try:
        yt_public_url = f"https://www.youtube.com/shorts/{yt_id}" if yt_id else YOUTUBE_URL
        # Step 1: create media container
        caption = f"{script.get('description','')}\n\n#Shorts #App #{APP_NAME.replace(' ','')}"
        cont_r = requests.post(
            f"https://graph.instagram.com/v21.0/{IG_USER_ID}/media",
            params={"media_type": "REELS", "video_url": yt_public_url,
                    "caption": caption[:2200], "share_to_feed": "true",
                    "access_token": IG_TOKEN}
        )
        container_id = cont_r.json().get("id")
        if container_id:
            # Step 2: wait for container to process, then publish
            info("Waiting for Instagram container to process...")
            for _ in range(12):
                time.sleep(10)
                status_r = requests.get(
                    f"https://graph.instagram.com/v21.0/{container_id}",
                    params={"fields": "status_code", "access_token": IG_TOKEN})
                if status_r.json().get("status_code") == "FINISHED":
                    break
            pub_r = requests.post(
                f"https://graph.instagram.com/v21.0/{IG_USER_ID}/media_publish",
                params={"creation_id": container_id, "access_token": IG_TOKEN})
            ig_media_id = pub_r.json().get("id")
            if ig_media_id:
                INSTAGRAM_URL = f"https://www.instagram.com/reels/{ig_media_id}"
                ok(f"Instagram Reels uploaded: {INSTAGRAM_URL}")
                with open(f"{OUTDIR}/instagram_id.txt", "w") as f: f.write(ig_media_id)
            else:
                warn(f"Instagram publish failed: {pub_r.text[:150]}")
        else:
            warn(f"Instagram container failed: {cont_r.text[:150]}")
    except Exception as e:
        warn(f"Instagram upload error: {str(e)[:100]}")

# ── Final Report ─────────────────────────────────────────────────────────────
n_chars = len(script["narration"])
n_scenes = len(script["scenes"])
costs = {
    "Gemini script":      0.002,
    "ElevenLabs TTS":     round((n_chars / 1000) * 0.30, 3),
    "Veo 3.1 video":      round(n_scenes * 0.50, 2),
    "Lyria 2 music":      0.01 if has_music else 0.0,
    "Imagen 4 thumbnail": 0.04,
}
total = sum(costs.values())

print(f"\n\033[1m{'='*54}")
print("   COMPLETE")
print(f"{'='*54}\033[0m")
print(f"Title:     {script['title']}")
print(f"Voice:     {chosen_voice}")
print(f"YouTube:   {YOUTUBE_URL or '(not uploaded)'}")
print(f"TikTok:    {TIKTOK_URL or '(not uploaded — add TIKTOK_ACCESS_TOKEN to .env)'}")
print(f"Instagram: {INSTAGRAM_URL or '(not uploaded — add INSTAGRAM_USER_ID + INSTAGRAM_ACCESS_TOKEN to .env)'}")
print(f"Files:     {OUTDIR}/")
print()
print("Cost estimate:")
for k, v in costs.items():
    print(f"  {k}: ${v:.3f}")
print(f"  TOTAL: ${total:.2f}")
print()

import glob
files = glob.glob(f"{OUTDIR}/*")
for fp in sorted(files):
    sz = os.path.getsize(fp)
    print(f"  {os.path.basename(fp):30s} {sz/1000:>8.0f}KB")
print()
