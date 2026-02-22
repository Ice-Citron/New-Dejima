"""
CLI entry point for the idea engine.

Usage:
  python -m src.cli scrape                    # Scrape Reddit for pain points
  python -m src.cli scrape --subreddits fitness,ADHD
  python -m src.cli analyze                   # Analyze latest scrape with LLM
  python -m src.cli analyze --file data/raw_scrape_20260221.json
  python -m src.cli rank                      # Rank and consolidate all ideas
  python -m src.cli evaluate "workout app that uses AI to adjust rest times"
  python -m src.cli status                    # Show idea pipeline summary
  python -m src.cli top                       # Show top 10 ideas
"""

import argparse
import json
import os
import sys
from pathlib import Path

from dotenv import load_dotenv

load_dotenv(Path(__file__).parent.parent / ".env")


def cmd_scrape(args):
    from .reddit_scraper import MultiScraper

    scraper = MultiScraper()
    keywords = args.keywords.split(",") if args.keywords else None
    include_google = not args.no_google

    print(f"Scraping for pain points (google={'on' if include_google else 'off'})...\n")
    results = scraper.scrape_all(keywords, include_google=include_google, time_range=args.time or "month")
    filepath = scraper.save_raw(results, "data")
    print(f"\nDone! {len(results)} posts saved to {filepath}")
    print(f"Next: python -m src.cli analyze --file {filepath}")


def cmd_analyze(args):
    from .analyzer import IdeaAnalyzer
    from .storage import IdeaStore

    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        print("Error: Set ANTHROPIC_API_KEY in .env")
        sys.exit(1)

    if args.file:
        filepath = Path(args.file)
    else:
        data_dir = Path("data")
        files = sorted(data_dir.glob("raw_scrape_*.json"), reverse=True)
        if not files:
            print("No scrape files found. Run 'scrape' first.")
            sys.exit(1)
        filepath = files[0]
        print(f"Using latest scrape: {filepath}")

    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)
    posts = data.get("posts", data) if isinstance(data, dict) else data

    top_posts = sorted(posts, key=lambda x: x.get("score", 0), reverse=True)[:args.limit or 30]
    print(f"Analyzing top {len(top_posts)} posts...")

    analyzer = IdeaAnalyzer(api_key)
    analyses = analyzer.analyze_posts(top_posts, batch_size=args.batch or 5)

    store = IdeaStore()
    added = store.add_ideas(analyses) if analyses else 0
    print(f"\nExtracted {len(analyses)} ideas, {added} new ones added to store")
    print(f"Next: python -m src.cli rank")


def cmd_rank(args):
    from .analyzer import IdeaAnalyzer
    from .storage import IdeaStore

    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        print("Error: Set ANTHROPIC_API_KEY in .env")
        sys.exit(1)

    store = IdeaStore()
    if not store.ideas:
        print("No ideas in store. Run 'scrape' then 'analyze' first.")
        sys.exit(1)

    print(f"Ranking {len(store.ideas)} ideas...")
    analyzer = IdeaAnalyzer(api_key)
    ranked = analyzer.rank_ideas(store.ideas)

    output = Path("data") / "ranked_ideas.json"
    with open(output, "w", encoding="utf-8") as f:
        json.dump(ranked, f, indent=2, ensure_ascii=False)

    print(f"\nResults saved to {output}")

    if ranked.get("top_ideas"):
        print("\n=== TOP IDEAS ===")
        for i, idea in enumerate(ranked["top_ideas"][:5], 1):
            name = idea.get("name", "Unnamed")
            conf = idea.get("confidence", "?")
            desc = idea.get("description", "")[:80]
            print(f"  {i}. {name} (confidence: {conf})")
            print(f"     {desc}")

    if ranked.get("themes"):
        print(f"\n=== THEMES ===")
        for theme in ranked["themes"]:
            if isinstance(theme, str):
                print(f"  - {theme}")
            else:
                print(f"  - {theme.get('name', theme)}")


def cmd_evaluate(args):
    from .analyzer import IdeaAnalyzer

    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        print("Error: Set ANTHROPIC_API_KEY in .env")
        sys.exit(1)

    idea = " ".join(args.idea)
    print(f"Evaluating: {idea}\n")

    analyzer = IdeaAnalyzer(api_key)
    result = analyzer.quick_evaluate(idea)

    print(json.dumps(result, indent=2))


def cmd_status(_args):
    from .storage import IdeaStore

    store = IdeaStore()
    summary = store.summary()

    if summary["total"] == 0:
        print("No ideas yet. Start with: python -m src.cli scrape")
        return

    print(f"\n=== Idea Pipeline ({summary['total']} total) ===")
    for status, count in summary["by_status"].items():
        print(f"  {status}: {count}")

    if summary["top_3"]:
        print(f"\nTop ideas:")
        for idea in summary["top_3"]:
            print(f"  - {idea['name']} (confidence: {idea['confidence']})")


def cmd_top(args):
    from .storage import IdeaStore

    store = IdeaStore()
    top = store.get_top(args.n or 10)

    if not top:
        print("No scored ideas yet. Run 'scrape' then 'analyze' first.")
        return

    print(f"\n=== Top {len(top)} Ideas ===\n")
    for i, idea in enumerate(top, 1):
        print(f"{i}. {idea.get('name', 'Unnamed')} — confidence: {idea.get('confidence', '?')}/10")
        print(f"   {idea.get('description', '')[:100]}")
        print(f"   Status: {idea.get('status', 'new')} | Source: r/{idea.get('source_subreddit', '?')}")
        print()


def cmd_build(args):
    from .builder import build_top_ideas

    n = args.n or 1
    dry_run = args.dry_run
    min_conf = args.min_confidence or 6

    print(f"Building top {n} idea(s) (min confidence: {min_conf}, dry_run: {dry_run})...\n")
    results = build_top_ideas(n=n, dry_run=dry_run, min_confidence=min_conf)

    if results:
        print(f"\n=== Build Summary ===")
        for r in results:
            status = "SENT" if r["success"] else "FAILED"
            print(f"  [{status}] {r['idea']}")


def cmd_pipeline(args):
    """Full autonomous pipeline: scrape → analyze → rank → build."""

    print("=" * 60)
    print("  IDEA ENGINE — FULL AUTONOMOUS PIPELINE")
    print("=" * 60)

    # Step 1: Scrape
    print("\n>>> STEP 1/4: Scraping for pain points...\n")
    from .reddit_scraper import MultiScraper
    scraper = MultiScraper()
    keywords = args.keywords.split(",") if args.keywords else None
    results = scraper.scrape_all(keywords, include_google=not args.no_google, time_range=args.time or "month")
    filepath = scraper.save_raw(results, "data")

    if len(results) < 5:
        print(f"\nOnly found {len(results)} posts — not enough data. Try different keywords.")
        sys.exit(1)

    # Step 2: Analyze
    print("\n>>> STEP 2/4: Analyzing posts with Claude...\n")
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        print("Error: Set ANTHROPIC_API_KEY in .env")
        sys.exit(1)

    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)
    posts = data.get("posts", [])
    top_posts = sorted(posts, key=lambda x: x.get("score", 0), reverse=True)[:30]

    from .analyzer import IdeaAnalyzer
    analyzer = IdeaAnalyzer(api_key)
    analyses = analyzer.analyze_posts(top_posts, batch_size=5)

    from .storage import IdeaStore
    store = IdeaStore()
    added = store.add_ideas(analyses)
    print(f"\nExtracted {len(analyses)} ideas, {added} new")

    if not store.ideas:
        print("No ideas extracted. Check your API key and try again.")
        sys.exit(1)

    # Step 3: Rank
    print("\n>>> STEP 3/4: Ranking ideas...\n")
    ranked = analyzer.rank_ideas(store.ideas)

    output = Path("data") / "ranked_ideas.json"
    with open(output, "w", encoding="utf-8") as f:
        json.dump(ranked, f, indent=2, ensure_ascii=False)

    top_ideas = ranked.get("top_ideas", [])
    if top_ideas:
        print("\nTop ideas:")
        for i, idea in enumerate(top_ideas[:5], 1):
            print(f"  {i}. {idea.get('name', 'Unnamed')} (confidence: {idea.get('confidence', '?')})")
    else:
        print("No ranked ideas produced.")
        sys.exit(1)

    # Step 4: Build
    n = args.build_top or 1
    dry_run = args.dry_run

    print(f"\n>>> STEP 4/4: Building top {n} idea(s)...\n")
    from .builder import build_top_ideas
    build_results = build_top_ideas(n=n, dry_run=dry_run, min_confidence=args.min_confidence or 6)

    print(f"\n{'='*60}")
    print("  PIPELINE COMPLETE")
    print(f"{'='*60}")
    print(f"  Posts scraped:  {len(results)}")
    print(f"  Ideas found:   {len(analyses)}")
    print(f"  Ideas ranked:  {len(top_ideas)}")
    print(f"  Builds sent:   {len(build_results)}")
    for r in build_results:
        status = "SENT" if r["success"] else "FAILED"
        print(f"    [{status}] {r['idea']}")
    print()


def main():
    parser = argparse.ArgumentParser(description="Idea Engine — find app ideas and build them autonomously")
    subparsers = parser.add_subparsers(dest="command")

    scrape_p = subparsers.add_parser("scrape", help="Scrape HN + Reddit (via Google) for pain points")
    scrape_p.add_argument("--keywords", help="Comma-separated custom keywords to search for")
    scrape_p.add_argument("--time", default="month", help="Time filter: day/week/month/year")
    scrape_p.add_argument("--no-google", action="store_true", help="Skip Google/Reddit search (HN only)")

    analyze_p = subparsers.add_parser("analyze", help="Analyze scraped posts with LLM")
    analyze_p.add_argument("--file", help="Specific scrape file to analyze")
    analyze_p.add_argument("--limit", type=int, default=30, help="Max posts to analyze")
    analyze_p.add_argument("--batch", type=int, default=5, help="Posts per LLM call")

    subparsers.add_parser("rank", help="Rank and consolidate all ideas")

    eval_p = subparsers.add_parser("evaluate", help="Quick-evaluate a single idea")
    eval_p.add_argument("idea", nargs="+", help="The app idea to evaluate")

    subparsers.add_parser("status", help="Show idea pipeline summary")

    top_p = subparsers.add_parser("top", help="Show top ideas")
    top_p.add_argument("-n", type=int, default=10, help="Number of ideas to show")

    build_p = subparsers.add_parser("build", help="Send top ideas to OpenClaw for building")
    build_p.add_argument("-n", type=int, default=1, help="Number of top ideas to build")
    build_p.add_argument("--dry-run", action="store_true", help="Show prompts without sending")
    build_p.add_argument("--min-confidence", type=int, default=6, help="Minimum confidence to build")

    pipe_p = subparsers.add_parser("pipeline", help="Full auto: scrape → analyze → rank → build")
    pipe_p.add_argument("--keywords", help="Comma-separated custom keywords")
    pipe_p.add_argument("--time", default="month", help="Time filter: day/week/month/year")
    pipe_p.add_argument("--no-google", action="store_true", help="Skip Google/Reddit (HN only)")
    pipe_p.add_argument("--build-top", type=int, default=1, help="How many top ideas to build")
    pipe_p.add_argument("--dry-run", action="store_true", help="Show build prompts without sending")
    pipe_p.add_argument("--min-confidence", type=int, default=6, help="Minimum confidence to build")

    args = parser.parse_args()

    commands = {
        "scrape": cmd_scrape,
        "analyze": cmd_analyze,
        "rank": cmd_rank,
        "evaluate": cmd_evaluate,
        "status": cmd_status,
        "top": cmd_top,
        "build": cmd_build,
        "pipeline": cmd_pipeline,
    }

    if args.command in commands:
        commands[args.command](args)
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
