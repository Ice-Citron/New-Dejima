"""
Product and niche research via Google Trends, Reddit, and web scraping.
Finds trending products and underserved niches for dropshipping.
"""

import json
import re
import time
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path


GOOGLE_TRENDS_DAILY_URL = "https://trends.google.com/trending/rss?geo=US"

TREND_SUBREDDITS = [
    "dropshipping", "ecommerce", "FulfillmentByAmazon",
    "Entrepreneur", "juststart", "ShopifyPros",
]

TREND_KEYWORDS = [
    "trending product 2026", "best dropshipping products",
    "viral product tiktok", "winning product dropshipping",
    "high demand low competition products",
]

HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
}


class ProductResearcher:
    """Scrapes multiple sources to find trending products and niches."""

    def __init__(self):
        self.results = []

    def search_google(self, query: str, num_results: int = 10) -> list[dict]:
        """Search Google for product trends and extract results."""
        encoded = urllib.parse.quote_plus(query)
        url = f"https://www.google.com/search?q={encoded}&num={num_results}"
        req = urllib.request.Request(url, headers=HEADERS)

        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                html = resp.read().decode("utf-8", errors="replace")

            results = []
            for match in re.finditer(r'<h3[^>]*>(.*?)</h3>', html, re.DOTALL):
                title = re.sub(r'<[^>]+>', '', match.group(1)).strip()
                if title and len(title) > 10:
                    results.append({
                        "title": title,
                        "source": "google",
                        "query": query,
                        "scraped_at": datetime.now(tz=timezone.utc).isoformat(),
                    })
            return results
        except Exception as e:
            print(f"  Google search failed for '{query}': {e}")
            return []

    def search_reddit(self, subreddit: str, query: str = "", limit: int = 25) -> list[dict]:
        """Search Reddit for product discussions."""
        if query:
            url = f"https://www.reddit.com/r/{subreddit}/search.json?q={urllib.parse.quote_plus(query)}&restrict_sr=1&sort=relevance&t=month&limit={limit}"
        else:
            url = f"https://www.reddit.com/r/{subreddit}/hot.json?limit={limit}"

        req = urllib.request.Request(url, headers={**HEADERS, "Accept": "application/json"})

        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                data = json.loads(resp.read().decode("utf-8"))

            posts = []
            for child in data.get("data", {}).get("children", []):
                post = child.get("data", {})
                if post.get("selftext") or post.get("title"):
                    posts.append({
                        "title": post.get("title", ""),
                        "text": (post.get("selftext", "") or "")[:500],
                        "score": post.get("score", 0),
                        "subreddit": subreddit,
                        "url": f"https://reddit.com{post.get('permalink', '')}",
                        "source": "reddit",
                        "scraped_at": datetime.now(tz=timezone.utc).isoformat(),
                    })
            return posts
        except Exception as e:
            print(f"  Reddit scrape failed for r/{subreddit}: {e}")
            return []

    def scrape_trends(self) -> list[dict]:
        """Scrape Google Trends RSS for currently trending topics."""
        req = urllib.request.Request(GOOGLE_TRENDS_DAILY_URL, headers=HEADERS)
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                xml = resp.read().decode("utf-8", errors="replace")

            trends = []
            for match in re.finditer(r'<title>(.*?)</title>', xml):
                title = match.group(1).strip()
                if title and title != "Daily Search Trends" and "Google" not in title:
                    trends.append({
                        "title": title,
                        "source": "google_trends",
                        "scraped_at": datetime.now(tz=timezone.utc).isoformat(),
                    })
            return trends
        except Exception as e:
            print(f"  Google Trends scrape failed: {e}")
            return []

    def research_all(
        self,
        custom_keywords: list[str] | None = None,
        include_reddit: bool = True,
        include_google: bool = True,
    ) -> list[dict]:
        """Run full research pipeline across all sources."""
        all_results = []
        keywords = custom_keywords or TREND_KEYWORDS

        # Google Trends
        print("  Scraping Google Trends...")
        trends = self.scrape_trends()
        all_results.extend(trends)
        print(f"    Found {len(trends)} trending topics")

        # Google search
        if include_google:
            for kw in keywords:
                print(f"  Searching Google: '{kw}'...")
                results = self.search_google(kw)
                all_results.extend(results)
                time.sleep(2)

        # Reddit
        if include_reddit:
            for sub in TREND_SUBREDDITS:
                print(f"  Scraping r/{sub}...")
                posts = self.search_reddit(sub, query="winning product")
                all_results.extend(posts)
                time.sleep(1)

        self.results = all_results
        return all_results

    def save_raw(self, results: list[dict], data_dir: str = "data") -> str:
        """Save raw research results to a timestamped JSON file."""
        data_path = Path(data_dir)
        data_path.mkdir(parents=True, exist_ok=True)

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filepath = data_path / f"raw_research_{timestamp}.json"

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump({
                "scraped_at": datetime.now(tz=timezone.utc).isoformat(),
                "total": len(results),
                "results": results,
            }, f, indent=2, ensure_ascii=False)

        return str(filepath)
