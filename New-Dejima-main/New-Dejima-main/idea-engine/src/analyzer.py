"""
LLM-based analyzer that takes raw Reddit posts and extracts:
1. The core problem/pain point
2. Potential app ideas that solve it
3. A confidence score and reasoning

Uses Anthropic's Claude API (same key as your OpenClaw setup).
"""

import json
from dataclasses import dataclass, field, asdict

import anthropic

ANALYSIS_PROMPT = """\
You are a product researcher analyzing Reddit posts to find viable mobile app ideas.

For each post, extract:
1. **problem**: The core pain point or unmet need (1-2 sentences)
2. **target_audience**: Who has this problem (be specific)
3. **app_ideas**: 1-3 concrete app ideas that solve this problem. For each idea:
   - name: A catchy app name
   - description: What it does (2-3 sentences)
   - key_features: 3-5 bullet points
   - monetization: How it could make money
   - complexity: "simple" | "medium" | "complex"
4. **market_signals**: Evidence this is a real need (upvotes, comment sentiment, frequency)
5. **confidence**: 1-10 rating of how viable this app idea is
6. **reasoning**: Why you gave that confidence score

Return valid JSON. Be brutally honest about viability — only rate 7+ if there's strong evidence of demand AND the idea is technically feasible as a mobile app.

Posts to analyze:
{posts_json}
"""

BATCH_ANALYSIS_PROMPT = """\
You are a product strategist reviewing a batch of analyzed app ideas.

Group similar ideas together, identify the strongest opportunities, and rank them.

For the top ideas, also suggest:
- **validation_steps**: How to quickly validate demand (specific subreddits to post in, questions to ask)
- **mvp_scope**: What the minimum viable product looks like
- **differentiator**: What would make this better than existing solutions

Input ideas:
{ideas_json}

Return a JSON object with:
- "top_ideas": ranked list of the best opportunities (max 10)
- "themes": common problem themes you noticed
- "rejected": ideas you'd skip and why (brief)
"""


import re


def _extract_json(text: str):
    """Extract JSON from LLM response, handling code fences and quirks."""
    text = re.sub(r"```(?:json)?\s*", "", text).strip()
    text = re.sub(r"```\s*$", "", text).strip()

    try:
        return json.loads(text)
    except json.JSONDecodeError:
        pass

    # Try finding the outermost JSON structure
    for start_char, end_char in [("[", "]"), ("{", "}")]:
        start = text.find(start_char)
        end = text.rfind(end_char)
        if start != -1 and end > start:
            try:
                return json.loads(text[start : end + 1])
            except json.JSONDecodeError:
                continue

    raise json.JSONDecodeError("No valid JSON found", text, 0)


def _normalize_ideas(parsed, source_posts: list[dict]) -> list[dict]:
    """Normalize LLM output into a flat list of ideas with names."""
    ideas = []

    # If it's a dict with keys like "post_1", "post_2", flatten it
    if isinstance(parsed, dict):
        if any(k.startswith("post") for k in parsed.keys()):
            parsed = list(parsed.values())
        elif "app_ideas" in parsed:
            parsed = parsed["app_ideas"]
        else:
            parsed = [parsed]

    if not isinstance(parsed, list):
        parsed = [parsed]

    for item in parsed:
        if not isinstance(item, dict):
            continue

        # The item might contain nested app_ideas
        nested_ideas = item.get("app_ideas", [])
        if nested_ideas and isinstance(nested_ideas, list):
            for idea in nested_ideas:
                if isinstance(idea, dict):
                    idea.setdefault("problem", item.get("problem", ""))
                    idea.setdefault("target_audience", item.get("target_audience", ""))
                    idea.setdefault("confidence", item.get("confidence", 0))
                    idea.setdefault("reasoning", item.get("reasoning", ""))
                    idea.setdefault("market_signals", item.get("market_signals", ""))
                    idea.setdefault("source_post_id", item.get("id", ""))
                    ideas.append(idea)
        elif item.get("name") or item.get("description"):
            ideas.append(item)
        elif item.get("problem"):
            # Post-level analysis without nested ideas — synthesize one
            ideas.append(item)

    # Ensure every idea has a name
    for i, idea in enumerate(ideas):
        if not idea.get("name"):
            desc = idea.get("description", idea.get("problem", f"Idea {i+1}"))
            idea["name"] = desc[:60].strip()

    return ideas


@dataclass
class AppIdea:
    name: str
    description: str
    key_features: list[str]
    monetization: str
    complexity: str
    source_post_id: str = ""
    source_subreddit: str = ""
    problem: str = ""
    target_audience: str = ""
    confidence: int = 0
    reasoning: str = ""
    market_signals: str = ""
    validation_steps: list[str] = field(default_factory=list)


class IdeaAnalyzer:
    def __init__(self, api_key: str, model: str = "claude-sonnet-4-20250514"):
        self.client = anthropic.Anthropic(api_key=api_key)
        self.model = model

    def analyze_posts(self, posts: list[dict], batch_size: int = 5) -> list[dict]:
        """Analyze a batch of Reddit posts and extract app ideas."""
        all_analyses = []

        for i in range(0, len(posts), batch_size):
            batch = posts[i : i + batch_size]
            print(f"Analyzing posts {i+1}-{min(i+batch_size, len(posts))} of {len(posts)}...")

            simplified = [
                {
                    "id": p["id"],
                    "source": p.get("source", p.get("subreddit", "unknown")),
                    "title": p["title"],
                    "body": p.get("body", "")[:500],
                    "score": p.get("score", 0),
                    "num_comments": p.get("num_comments", 0),
                }
                for p in batch
            ]

            prompt = ANALYSIS_PROMPT.format(posts_json=json.dumps(simplified, indent=2))

            try:
                response = self.client.messages.create(
                    model=self.model,
                    max_tokens=4096,
                    messages=[{"role": "user", "content": prompt}],
                )

                text = response.content[0].text
                parsed = _extract_json(text)
                ideas = _normalize_ideas(parsed, batch)
                all_analyses.extend(ideas)

            except json.JSONDecodeError as e:
                print(f"  Warning: failed to parse LLM response: {e}")
                print(f"  Raw response: {text[:200]}...")
            except Exception as e:
                print(f"  Error analyzing batch: {e}")

        return all_analyses

    def rank_ideas(self, analyses: list[dict]) -> dict:
        """Take all analyzed ideas and rank/consolidate them."""
        if not analyses:
            return {"top_ideas": [], "themes": [], "rejected": []}

        prompt = BATCH_ANALYSIS_PROMPT.format(ideas_json=json.dumps(analyses, indent=2))

        try:
            response = self.client.messages.create(
                model=self.model,
                max_tokens=4096,
                messages=[{"role": "user", "content": prompt}],
            )

            text = response.content[0].text
            return _extract_json(text)

        except Exception as e:
            print(f"Error ranking ideas: {e}")
            return {"top_ideas": analyses[:10], "themes": [], "rejected": [], "error": str(e)}

    def quick_evaluate(self, idea_description: str) -> dict:
        """Quick evaluation of a single app idea."""
        prompt = f"""\
Evaluate this app idea. Return JSON with:
- viable (bool), confidence (1-10), reasoning (string)
- competitors (list of existing apps that do something similar)
- unique_angle (what would make this stand out)
- estimated_effort (days for an MVP)
- target_subreddits (where to validate this idea)

Idea: {idea_description}"""

        response = self.client.messages.create(
            model=self.model,
            max_tokens=2048,
            messages=[{"role": "user", "content": prompt}],
        )

        text = response.content[0].text
        json_start = text.find("{")
        json_end = text.rfind("}") + 1
        return json.loads(text[json_start:json_end])
