# API Setup Guides — AI Video Creator

Step-by-step instructions to get every key needed for autonomous video generation and publishing.

---

## 1. Google API Key (Gemini / Veo 2 / Imagen 4)

You likely already have this. Check if `GEMINI_API_KEY` is set somewhere in your existing keys.

If not:

1. Go to **https://aistudio.google.com/apikey**
2. Click **"Create API key"**
3. Select an existing Google Cloud project (or create a new one named `dejima`)
4. Copy the key
5. Paste it into `.env` next to `GEMINI_API_KEY=`

**Test it:**
```bash
source .env
curl -s "https://generativelanguage.googleapis.com/v1beta/models?key=$GEMINI_API_KEY" | python3 -c "import json,sys; models = json.loads(sys.stdin.read()).get('models',[]); print(f'Found {len(models)} models — key works!')"
```

---

## 2. YouTube OAuth (one-time browser auth)

This is the most complex setup. Do it once, save the refresh token, never touch it again.

### Step 1 — Enable YouTube Data API v3

1. Go to **https://console.cloud.google.com/**
2. Select or create your project (`dejima`)
3. Go to **APIs & Services → Library**
4. Search for **"YouTube Data API v3"**
5. Click **Enable**

### Step 2 — Create OAuth 2.0 Credentials

1. Go to **APIs & Services → Credentials**
2. Click **"Create Credentials" → "OAuth client ID"**
3. If prompted, configure the OAuth consent screen first:
   - User type: **External**
   - App name: `Dejima`
   - Add your Gmail as test user
4. Application type: **Desktop app**
5. Name: `Dejima YouTube Uploader`
6. Click **Create**
7. Download the JSON — or copy the **Client ID** and **Client Secret**
8. Paste them into `.env`:
   ```
   YOUTUBE_CLIENT_ID=your_client_id_here
   YOUTUBE_CLIENT_SECRET=your_client_secret_here
   ```

### Step 3 — Get the Refresh Token (one-time browser auth)

Run this command in terminal (replace with your actual client ID and secret):

```bash
source .env

# Step A: Get the auth URL
AUTH_URL="https://accounts.google.com/o/oauth2/v2/auth?client_id=${YOUTUBE_CLIENT_ID}&redirect_uri=urn:ietf:wg:oauth:2.0:oob&response_type=code&scope=https://www.googleapis.com/auth/youtube.upload https://www.googleapis.com/auth/youtube&access_type=offline&prompt=consent"

echo "Open this URL in your browser:"
echo "$AUTH_URL"
```

4. Open the URL in your browser
5. Sign in with your Google account
6. Allow the permissions
7. Copy the **authorization code** shown on screen

```bash
# Step B: Exchange code for refresh token
AUTH_CODE="PASTE_CODE_HERE"

curl -s -X POST "https://oauth2.googleapis.com/token" \
  -d "code=$AUTH_CODE" \
  -d "client_id=$YOUTUBE_CLIENT_ID" \
  -d "client_secret=$YOUTUBE_CLIENT_SECRET" \
  -d "redirect_uri=urn:ietf:wg:oauth:2.0:oob" \
  -d "grant_type=authorization_code"
```

8. From the JSON response, copy the `refresh_token` value
9. Paste it into `.env`:
   ```
   YOUTUBE_REFRESH_TOKEN=your_refresh_token_here
   ```

**Test it:**
```bash
source .env
ACCESS_TOKEN=$(curl -s -X POST "https://oauth2.googleapis.com/token" \
  -d "client_id=$YOUTUBE_CLIENT_ID" \
  -d "client_secret=$YOUTUBE_CLIENT_SECRET" \
  -d "refresh_token=$YOUTUBE_REFRESH_TOKEN" \
  -d "grant_type=refresh_token" \
  | python3 -c "import json,sys; print(json.loads(sys.stdin.read()).get('access_token','FAILED'))")

echo "Access token: ${ACCESS_TOKEN:0:20}..."
```

---

## 3. TikTok API

### Step 1 — Create a TikTok Developer Account

1. Go to **https://developers.tiktok.com/**
2. Log in with your TikTok account
3. Click **"Manage apps" → "Connect an app"**
4. App name: `Dejima`
5. Category: **Entertainment**

### Step 2 — Enable Content Posting API

1. In your app dashboard, go to **"Products"**
2. Add **"Content Posting API"**
3. Set redirect URI to: `http://localhost:8080/callback`
4. Copy your **Client Key** and **Client Secret** into `.env`

### Step 3 — Get Access Token

TikTok uses OAuth 2.0. For a personal account, use this flow:

```bash
source .env

# Get auth URL
echo "https://www.tiktok.com/v2/auth/authorize/?client_key=${TIKTOK_CLIENT_KEY}&response_type=code&scope=video.publish,video.upload&redirect_uri=http://localhost:8080/callback"
```

Open the URL → authorize → copy the `code` from the redirect URL → exchange for access token:

```bash
CODE="PASTE_CODE_HERE"
curl -X POST "https://open.tiktokapis.com/v2/oauth/token/" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_key=$TIKTOK_CLIENT_KEY&client_secret=$TIKTOK_CLIENT_SECRET&code=$CODE&grant_type=authorization_code&redirect_uri=http://localhost:8080/callback"
```

Paste the `access_token` into `.env` as `TIKTOK_ACCESS_TOKEN`.

**Note:** TikTok access tokens expire. For production, implement refresh token rotation.

---

## 4. ElevenLabs (already configured)

Key is already in `.env`. To change the voice:

1. Go to **https://elevenlabs.io/voice-library**
2. Find a voice you like
3. Click it → copy the Voice ID from the URL or settings
4. Update `ELEVENLABS_VOICE_ID=` in `.env`

Popular voice IDs:
- `21m00Tcm4TlvDq8ikWAM` — Rachel (calm, professional female)
- `AZnzlk1XvdvUeBnXmlld` — Domi (energetic female)
- `ErXwobaYiN019PkySvjV` — Antoni (well-rounded male)
- `VR6AewLTigWG4xSOukaG` — Arnold (crisp male)

---

## Quick Verification Script

Run this to check which keys are configured:

```bash
source /Users/yacine/Desktop/Dejima2/New-Dejima/.env

check() {
  if [ -n "${!1}" ]; then
    echo "✓ $1 — set"
  else
    echo "✗ $1 — MISSING"
  fi
}

check GEMINI_API_KEY
check ELEVENLABS_API_KEY
check ELEVENLABS_VOICE_ID
check YOUTUBE_CLIENT_ID
check YOUTUBE_CLIENT_SECRET
check YOUTUBE_REFRESH_TOKEN
check TIKTOK_CLIENT_KEY
check TIKTOK_ACCESS_TOKEN
```
