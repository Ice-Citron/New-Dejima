"""
Bridge between idea-engine and OpenClaw's android-app-builder.
Converts a ranked idea into a build prompt and sends it to the agent.
"""

import json
import subprocess
import sys
from pathlib import Path


PROMPT_TEMPLATE = """\
Build me an Android app called "{name}".

{description}

Key features:
{features}

Target audience: {audience}

Follow the android-app-builder skill and produce a working APK. \
Use Material 3, Jetpack Compose, Kotlin. Keep the MVP simple and focused."""


# The actual OpenClaw repo with build output (not the New-Dejima submodule)
OPENCLAW_DIR = Path(__file__).parent.parent.parent.parent / "openclaw"


def idea_to_prompt(idea: dict) -> str:
    """Convert a ranked idea dict into a build prompt for OpenClaw."""
    name = idea.get("name", "Untitled App")
    description = idea.get("description", idea.get("problem", ""))
    audience = idea.get("target_audience", "general users")

    features_list = idea.get("key_features", [])
    if not features_list and idea.get("mvp_scope"):
        features_list = [idea["mvp_scope"]]
    features = "\n".join(f"- {f}" for f in features_list) if features_list else "- Core functionality as described above"

    return PROMPT_TEMPLATE.format(
        name=name,
        description=description,
        features=features,
        audience=audience,
    )


def load_gateway_token() -> str | None:
    """Load the OpenClaw gateway token from config.
    Tries ~/.openclaw-dev first (dev gateway), then ~/.openclaw.
    """
    import os
    # Dev config takes priority (matches the running gateway)
    for config_dir in [".openclaw-dev", ".openclaw"]:
        config_path = Path.home() / config_dir / "openclaw.json"
        if config_path.exists():
            config = json.loads(config_path.read_text(encoding="utf-8"))
            token = config.get("gateway", {}).get("auth", {}).get("token")
            if token:
                return token
    return None


def send_to_openclaw(prompt: str, agent: str = "main", dry_run: bool = False) -> bool:
    """Send a build prompt to the OpenClaw agent."""
    import os

    token = load_gateway_token()
    if token:
        os.environ["OPENCLAW_GATEWAY_TOKEN"] = token

    # Use dev config dir so CLI connects to the right gateway
    os.environ["OPENCLAW_CONFIG_DIR"] = str(Path.home() / ".openclaw-dev")

    if dry_run:
        print(f"\n{'='*60}")
        print("DRY RUN — would send this prompt to OpenClaw:")
        print(f"{'='*60}")
        print(prompt)
        print(f"{'='*60}\n")
        return True

    openclaw_mjs = OPENCLAW_DIR / "openclaw.mjs"
    if not openclaw_mjs.exists():
        print(f"Error: OpenClaw not found at {OPENCLAW_DIR}")
        return False

    print(f"\nSending to OpenClaw agent '{agent}'...")
    print(f"Prompt preview: {prompt[:100]}...\n")

    result = subprocess.run(
        ["node", str(openclaw_mjs), "agent", "--agent", agent, "--message", prompt],
        cwd=str(OPENCLAW_DIR),
        text=True,
    )

    return result.returncode == 0


def build_top_ideas(
    ranked_path: str | Path = "data/ranked_ideas.json",
    ideas_path: str | Path = "data/ideas.json",
    n: int = 1,
    dry_run: bool = False,
    min_confidence: int = 6,
) -> list[dict]:
    """Load top N ideas and send each to OpenClaw for building."""

    # Prefer ideas.json (has confidence scores) over ranked_ideas.json
    ranked_path = Path(ranked_path)
    ideas_path = Path(ideas_path)

    ideas = []
    if ideas_path.exists():
        data = json.loads(ideas_path.read_text(encoding="utf-8"))
        raw = data.get("ideas", data) if isinstance(data, dict) else data
        ideas = sorted(raw, key=lambda x: x.get("confidence", 0), reverse=True)
    elif ranked_path.exists():
        data = json.loads(ranked_path.read_text(encoding="utf-8"))
        ideas = data.get("top_ideas", [])

    if not ideas:
        print("No ideas found. Run the full pipeline first: python -m src.cli pipeline")
        return []

    # Filter by confidence
    viable = [i for i in ideas if (i.get("confidence") or 0) >= min_confidence]
    if not viable:
        print(f"No ideas with confidence >= {min_confidence}. Lowering threshold...")
        viable = ideas[:n]

    selected = viable[:n]
    built = []

    for i, idea in enumerate(selected, 1):
        name = idea.get("name", "Unnamed")
        conf = idea.get("confidence", "?")
        print(f"\n{'='*60}")
        print(f"Building idea {i}/{len(selected)}: {name} (confidence: {conf})")
        print(f"{'='*60}")

        prompt = idea_to_prompt(idea)
        success = send_to_openclaw(prompt, dry_run=dry_run)

        idea["build_status"] = "sent" if success else "failed"
        built.append({"idea": name, "prompt": prompt, "success": success})

    return built
