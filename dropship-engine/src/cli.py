"""
CLI entry point for the dropship engine.

Usage:
  python -m src.cli research                   # Scrape for trending products/niches
  python -m src.cli analyze                    # LLM-analyze research data into niches
  python -m src.cli source                     # Find suppliers + calculate margins
  python -m src.cli build-store                # Generate store via Lovable
  python -m src.cli evaluate "LED desk lamp"   # Quick-evaluate a single product
  python -m src.cli status                     # Show pipeline summary
  python -m src.cli pipeline                   # Full auto: research → analyze → source → build
"""

import argparse
import json
import os
import sys
from pathlib import Path

from dotenv import load_dotenv

load_dotenv(Path(__file__).parent.parent / ".env")


def cmd_research(args):
    from .researcher import ProductResearcher

    researcher = ProductResearcher()
    keywords = args.keywords.split(",") if args.keywords else None

    print("Researching trending products and niches...\n")
    results = researcher.research_all(
        custom_keywords=keywords,
        include_reddit=not args.no_reddit,
        include_google=not args.no_google,
    )
    filepath = researcher.save_raw(results, "data")
    print(f"\nDone! {len(results)} results saved to {filepath}")
    print(f"Next: python -m src.cli analyze --file {filepath}")


def cmd_analyze(args):
    from .analyzer import NicheAnalyzer
    from .storage import DropshipStore

    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        print("Error: Set ANTHROPIC_API_KEY in .env")
        sys.exit(1)

    if args.file:
        filepath = Path(args.file)
    else:
        data_dir = Path("data")
        files = sorted(data_dir.glob("raw_research_*.json"), reverse=True)
        if not files:
            print("No research files found. Run 'research' first.")
            sys.exit(1)
        filepath = files[0]
        print(f"Using latest research: {filepath}")

    with open(filepath, "r", encoding="utf-8") as f:
        data = json.load(f)
    results = data.get("results", data) if isinstance(data, dict) else data

    print(f"Analyzing {len(results)} results for profitable niches...\n")
    analyzer = NicheAnalyzer(api_key)
    niches = analyzer.analyze_niches(results, batch_size=args.batch or 20)
    ranked = analyzer.rank_niches(niches)

    store = DropshipStore()
    added = store.add_niches(ranked)
    print(f"\nFound {len(ranked)} niches, {added} new ones added")

    if ranked:
        print("\n=== TOP NICHES ===")
        for i, niche in enumerate(ranked[:5], 1):
            print(f"  {i}. {niche.get('name', 'Unknown')} (confidence: {niche.get('confidence', '?')}/10)")
            print(f"     {niche.get('description', '')[:80]}")
            print(f"     Competition: {niche.get('competition_level', '?')} | Margin: {niche.get('estimated_margin', '?')}")
            print()

    print("Next: python -m src.cli source")


def cmd_source(args):
    from .sourcer import ProductSourcer
    from .storage import DropshipStore

    store = DropshipStore()
    niches = store.get_top_niches(args.top or 3)

    if not niches:
        print("No niches found. Run 'research' then 'analyze' first.")
        sys.exit(1)

    print(f"Sourcing products for top {len(niches)} niches...\n")
    sourcer = ProductSourcer()
    products = sourcer.source_from_niches(niches, top_n=args.top or 3)

    added = store.add_products(products)
    print(f"\nFound {len(products)} products with 30%+ margins, {added} saved")

    if products:
        print("\n=== BEST PRODUCTS ===")
        for i, p in enumerate(products[:10], 1):
            print(f"  {i}. {p['name'][:50]}")
            print(f"     Cost: ${p['cogs']:.2f} → Sell: ${p['sell_price']:.2f} → Profit: ${p['net_profit']:.2f} ({p['net_margin']})")
            print()

    print("Next: python -m src.cli build-store")


def cmd_build_store(args):
    from .storage import DropshipStore
    from .store_builder import StoreBuilder

    store = DropshipStore()
    niches = store.get_top_niches(1)
    products = store.get_profitable_products(min_margin=0.30)

    if not niches:
        print("No niches found. Run the full pipeline first.")
        sys.exit(1)

    niche = niches[0]
    niche_products = [p for p in products if p.get("niche", "").lower() == niche.get("name", "").lower()]
    if not niche_products:
        niche_products = products[:8]

    builder = StoreBuilder()
    result = builder.launch_store(niche, niche_products, dry_run=args.dry_run)

    if not args.dry_run:
        store.add_store(result)
        print(f"\n  Store '{result['store_name']}' launched via Lovable!")
        print(f"  Check your browser — Lovable is building it now.")


def cmd_evaluate(args):
    from .analyzer import NicheAnalyzer

    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        print("Error: Set ANTHROPIC_API_KEY in .env")
        sys.exit(1)

    product = " ".join(args.product)
    print(f"Evaluating: {product}\n")

    analyzer = NicheAnalyzer(api_key)
    result = analyzer.evaluate_product(product)
    print(json.dumps(result, indent=2))


def cmd_status(_args):
    from .storage import DropshipStore

    store = DropshipStore()
    summary = store.summary()

    print("\n=== Dropship Engine Status ===\n")

    n = summary["niches"]
    print(f"  Niches: {n['total']} total")
    if n["top_3"]:
        for niche in n["top_3"]:
            print(f"    - {niche['name']} (confidence: {niche['confidence']})")

    p = summary["products"]
    print(f"\n  Products: {p['total']} sourced, {p['profitable']} profitable (30%+ margin)")
    if p["best"]:
        b = p["best"]
        print(f"    Best: {b.get('name', 'Unknown')[:40]} — ${b.get('net_profit', 0):.2f} profit/sale")

    s = summary["stores"]
    print(f"\n  Stores: {s['total']} built")
    if s["latest"]:
        print(f"    Latest: {s['latest']}")

    if n["total"] == 0:
        print("\n  Get started: python -m src.cli research")
    print()


def cmd_pipeline(args):
    """Full autonomous pipeline: research > analyze > source > build-store."""

    print("=" * 60)
    print("  DROPSHIP ENGINE — FULL AUTONOMOUS PIPELINE")
    print("=" * 60)

    # Step 1: Research
    print("\n>>> STEP 1/4: Researching trending products...\n")
    from .researcher import ProductResearcher
    researcher = ProductResearcher()
    keywords = args.keywords.split(",") if args.keywords else None
    results = researcher.research_all(
        custom_keywords=keywords,
        include_reddit=not args.no_reddit,
        include_google=not args.no_google,
    )
    filepath = researcher.save_raw(results, "data")

    if len(results) < 3:
        print(f"\nOnly found {len(results)} results — not enough data.")
        sys.exit(1)

    # Step 2: Analyze
    print("\n>>> STEP 2/4: Analyzing niches with Claude...\n")
    api_key = os.environ.get("ANTHROPIC_API_KEY")
    if not api_key:
        print("Error: Set ANTHROPIC_API_KEY in .env")
        sys.exit(1)

    from .analyzer import NicheAnalyzer
    analyzer = NicheAnalyzer(api_key)
    niches = analyzer.analyze_niches(results, batch_size=20)
    ranked = analyzer.rank_niches(niches)

    from .storage import DropshipStore
    store = DropshipStore()
    added = store.add_niches(ranked)
    print(f"\nFound {len(ranked)} niches, {added} new")

    if not ranked:
        print("No niches identified. Try different keywords.")
        sys.exit(1)

    print("\nTop niches:")
    for i, n in enumerate(ranked[:3], 1):
        print(f"  {i}. {n.get('name', 'Unknown')} (confidence: {n.get('confidence', '?')})")

    # Step 3: Source
    print("\n>>> STEP 3/4: Sourcing products from suppliers...\n")
    from .sourcer import ProductSourcer
    sourcer = ProductSourcer()
    products = sourcer.source_from_niches(ranked[:3], top_n=3)
    store.add_products(products)
    print(f"\nFound {len(products)} profitable products")

    # Step 4: Build store
    top_niche = ranked[0]
    niche_products = [p for p in products if p.get("niche", "").lower() == top_niche.get("name", "").lower()]
    if not niche_products:
        niche_products = products[:8]

    print(f"\n>>> STEP 4/4: Building store for '{top_niche.get('name')}'...\n")
    from .store_builder import StoreBuilder
    builder = StoreBuilder()
    result = builder.launch_store(top_niche, niche_products, dry_run=args.dry_run)

    if not args.dry_run:
        store.add_store(result)

    print(f"\n{'='*60}")
    print("  PIPELINE COMPLETE")
    print(f"{'='*60}")
    print(f"  Research results:  {len(results)}")
    print(f"  Niches found:     {len(ranked)}")
    print(f"  Products sourced: {len(products)}")
    print(f"  Store:            {result.get('store_name', 'N/A')}")
    if args.dry_run:
        print(f"  Mode:             DRY RUN (no store launched)")
    else:
        print(f"  Status:           Launched in browser via Lovable")
    print()


def main():
    parser = argparse.ArgumentParser(
        description="Dropship Engine — research niches, source products, build stores autonomously"
    )
    subparsers = parser.add_subparsers(dest="command")

    # research
    research_p = subparsers.add_parser("research", help="Scrape for trending products and niches")
    research_p.add_argument("--keywords", help="Comma-separated custom keywords")
    research_p.add_argument("--no-reddit", action="store_true", help="Skip Reddit scraping")
    research_p.add_argument("--no-google", action="store_true", help="Skip Google search")

    # analyze
    analyze_p = subparsers.add_parser("analyze", help="LLM-analyze research into niches")
    analyze_p.add_argument("--file", help="Specific research file to analyze")
    analyze_p.add_argument("--batch", type=int, default=20, help="Results per LLM call")

    # source
    source_p = subparsers.add_parser("source", help="Find suppliers and calculate margins")
    source_p.add_argument("--top", type=int, default=3, help="Number of top niches to source")

    # build-store
    build_p = subparsers.add_parser("build-store", help="Generate store via Lovable")
    build_p.add_argument("--dry-run", action="store_true", help="Show prompt without launching")

    # evaluate
    eval_p = subparsers.add_parser("evaluate", help="Quick-evaluate a single product idea")
    eval_p.add_argument("product", nargs="+", help="Product description to evaluate")

    # status
    subparsers.add_parser("status", help="Show pipeline summary")

    # pipeline
    pipe_p = subparsers.add_parser("pipeline", help="Full auto: research > analyze > source > build")
    pipe_p.add_argument("--keywords", help="Comma-separated custom keywords")
    pipe_p.add_argument("--no-reddit", action="store_true", help="Skip Reddit")
    pipe_p.add_argument("--no-google", action="store_true", help="Skip Google")
    pipe_p.add_argument("--dry-run", action="store_true", help="Show store prompt without launching")

    args = parser.parse_args()

    commands = {
        "research": cmd_research,
        "analyze": cmd_analyze,
        "source": cmd_source,
        "build-store": cmd_build_store,
        "evaluate": cmd_evaluate,
        "status": cmd_status,
        "pipeline": cmd_pipeline,
    }

    if args.command in commands:
        commands[args.command](args)
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
