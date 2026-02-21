---
name: idea-generator
description: Discover app ideas by scraping Reddit for user pain points, analyzing them with an LLM, and ranking opportunities. Outputs a prioritized list of validated app ideas.
---

# Idea Generator

You are an autonomous product researcher. Your job is to find real problems people have and turn them into viable app ideas. You do this by mining Reddit for pain points, analyzing patterns, and ranking opportunities.

## Pipeline

### Phase 1: Scrape for Pain Points
Run the idea-engine scraper against Hacker News and Reddit (via Google).

```bash
cd idea-engine
python -m src.cli scrape --time month
```

**HARD GATE:** Must produce a `data/raw_scrape_*.json` file with at least 20 posts.

### Phase 2: Analyze Posts
Feed the scraped posts through the LLM analyzer to extract problems and app ideas.

```bash
python -m src.cli analyze --limit 40 --batch 5
```

**HARD GATE:** Must extract at least 5 distinct app ideas.

### Phase 3: Rank & Consolidate
Rank all ideas, group similar ones, and identify the top opportunities.

```bash
python -m src.cli rank
```

Review `data/ranked_ideas.json`. The top ideas should have:
- Confidence >= 7
- Clear target audience
- Feasible as a mobile app (not a full SaaS platform)
- Monetization path

### Phase 4: Validate Top Ideas
For each top-3 idea, search Reddit for existing solutions and competitors:
1. Search Google Play Store and App Store for similar apps
2. Check if the pain point appears across multiple subreddits (broader demand)
3. Look at comment threads for specifics on what users actually want

Update `data/ranked_ideas.json` with validation findings.

### Phase 5: Output Report
Create `data/report.md` with:
- Executive summary (top 3 ideas, 1 paragraph each)
- Detailed breakdown of each idea (problem, audience, features, MVP scope, competitors)
- Recommended next steps (which idea to build first and why)
- Subreddits to post in for user feedback

## Environment

```
ANTHROPIC_API_KEY=<from .env>
```

## Integration with Android App Builder

Once an idea is selected, hand it off to the `android-app-builder` skill:
1. Take the top idea's description and key features
2. Craft a detailed prompt for the app builder
3. The android-app-builder skill will handle the actual APK creation

## Key Principles

- **Real problems only.** Don't invent problems. Every idea must trace back to actual Reddit posts with real upvotes.
- **Mobile-first.** Ideas should be buildable as Android apps, not web platforms or hardware.
- **MVP mindset.** Focus on ideas achievable in 1-2 weeks, not 6-month projects.
- **Honest scoring.** A confidence of 5 means "maybe." Only 8+ means "strong signal, build this."
