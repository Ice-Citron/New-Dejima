"""
Multi-source scraper for discovering user pain points and app ideas.

Sources:
  1. Hacker News (free API, no auth)
  2. Google search for Reddit posts (via site:reddit.com queries)
  3. Google Play Store reviews (1-star reviews = pain points)
"""

import json
import time
import urllib.request
import urllib.parse
from datetime import datetime, timezone
from pathlib import Path

PAIN_KEYWORDS = [
    "I wish there was an app",
    "why is there no app",
    "someone should build",
    "is there an app for",
    "frustrated with",
    "looking for an app",
    "need a better app",
    "there should be",
    "any alternative to",
    "looking for alternative",
    "sick of using",
    "does anyone else struggle with",
    "is there a better way to",
    "can't find a good app for",
]


class HackerNewsScraper:
    """Scrape Hacker News via their free, no-auth API."""

    BASE_URL = "https://hn.algolia.com/api/v1"

    def _get_json(self, url: str) -> dict:
        req = urllib.request.Request(url, headers={"User-Agent": "idea-engine/1.0"})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode())

    def search(
        self,
        keywords: list[str] | None = None,
        tags: str = "story",
        time_range: str = "month",
        hits_per_page: int = 30,
    ) -> list[dict]:
        """Search HN stories/comments for pain-point keywords."""
        keywords = keywords or PAIN_KEYWORDS
        results = []
        seen_ids = set()

        time_map = {
            "day": "last_24h",
            "week": "past_week",
            "month": "past_month",
            "year": "past_year",
        }
        numeric_filter = f"created_at_i>{int(time.time()) - 86400 * 30}"

        for keyword in keywords:
            query = urllib.parse.quote(keyword)
            url = (
                f"{self.BASE_URL}/search?query={query}"
                f"&tags={tags}"
                f"&hitsPerPage={hits_per_page}"
                f"&numericFilters={numeric_filter}"
            )

            try:
                data = self._get_json(url)
                for hit in data.get("hits", []):
                    obj_id = hit.get("objectID", "")
                    if obj_id in seen_ids:
                        continue
                    seen_ids.add(obj_id)

                    results.append({
                        "id": obj_id,
                        "source": "hackernews",
                        "title": hit.get("title", ""),
                        "body": hit.get("story_text") or hit.get("comment_text") or "",
                        "url": hit.get("url") or f"https://news.ycombinator.com/item?id={obj_id}",
                        "score": hit.get("points", 0) or 0,
                        "num_comments": hit.get("num_comments", 0) or 0,
                        "created_utc": hit.get("created_at", ""),
                        "keyword_matched": keyword,
                        "author": hit.get("author", ""),
                    })
            except Exception as e:
                print(f"  Warning: HN search for '{keyword}' failed: {e}")

            time.sleep(0.3)

        # Also search comments for pain points
        for keyword in keywords[:8]:
            query = urllib.parse.quote(keyword)
            url = (
                f"{self.BASE_URL}/search?query={query}"
                f"&tags=comment"
                f"&hitsPerPage=20"
                f"&numericFilters={numeric_filter}"
            )

            try:
                data = self._get_json(url)
                for hit in data.get("hits", []):
                    obj_id = hit.get("objectID", "")
                    if obj_id in seen_ids:
                        continue
                    seen_ids.add(obj_id)

                    comment_text = hit.get("comment_text", "")
                    if len(comment_text) < 50:
                        continue

                    results.append({
                        "id": obj_id,
                        "source": "hackernews_comment",
                        "title": f"Comment on: {hit.get('story_title', 'Unknown')}",
                        "body": comment_text[:2000],
                        "url": f"https://news.ycombinator.com/item?id={obj_id}",
                        "score": hit.get("points", 0) or 0,
                        "num_comments": 0,
                        "created_utc": hit.get("created_at", ""),
                        "keyword_matched": keyword,
                        "author": hit.get("author", ""),
                    })
            except Exception as e:
                print(f"  Warning: HN comment search for '{keyword}' failed: {e}")

            time.sleep(0.3)

        results.sort(key=lambda x: x["score"], reverse=True)
        return results


class GoogleRedditScraper:
    """
    Search Google for Reddit posts about pain points.
    Uses site:reddit.com queries. No Reddit API needed.

    Note: For heavy use, consider SerpAPI ($50/mo) or ScraperAPI
    to avoid Google rate limits. For light use, direct requests work.
    """

    def search(self, keywords: list[str] | None = None, limit: int = 20) -> list[dict]:
        keywords = keywords or PAIN_KEYWORDS[:8]
        results = []
        seen_urls = set()

        for keyword in keywords:
            query = urllib.parse.quote(f"site:reddit.com {keyword}")
            url = f"https://www.google.com/search?q={query}&num={limit}"

            try:
                req = urllib.request.Request(url, headers={
                    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                })
                with urllib.request.urlopen(req, timeout=10) as resp:
                    html = resp.read().decode("utf-8", errors="replace")

                # Extract Reddit URLs from search results (basic parsing)
                import re
                reddit_urls = re.findall(r'https?://(?:www\.)?reddit\.com/r/\w+/comments/\w+/[^"&\s]+', html)

                for reddit_url in reddit_urls:
                    clean_url = reddit_url.split("&")[0].split('"')[0]
                    if clean_url in seen_urls:
                        continue
                    seen_urls.add(clean_url)

                    # Extract subreddit and title from URL
                    parts = clean_url.split("/")
                    subreddit = ""
                    title_slug = ""
                    for i, p in enumerate(parts):
                        if p == "r" and i + 1 < len(parts):
                            subreddit = parts[i + 1]
                        if p == "comments" and i + 2 < len(parts):
                            title_slug = parts[i + 2] if i + 2 < len(parts) else ""

                    results.append({
                        "id": title_slug or clean_url,
                        "source": "reddit_via_google",
                        "subreddit": subreddit,
                        "title": title_slug.replace("_", " ").title() if title_slug else "",
                        "body": "",
                        "url": clean_url,
                        "score": 0,
                        "num_comments": 0,
                        "created_utc": "",
                        "keyword_matched": keyword,
                    })

            except Exception as e:
                print(f"  Warning: Google search for '{keyword}' failed: {e}")

            time.sleep(2)  # be polite to Google

        return results


class MultiScraper:
    """Aggregates results from all scrapers."""

    def __init__(self):
        self.hn = HackerNewsScraper()
        self.google = GoogleRedditScraper()

    def scrape_all(
        self,
        keywords: list[str] | None = None,
        include_google: bool = True,
        time_range: str = "month",
    ) -> list[dict]:
        all_results = []

        print("=== Scanning Hacker News ===")
        hn_results = self.hn.search(keywords, time_range=time_range)
        print(f"  Found {len(hn_results)} posts/comments")
        all_results.extend(hn_results)

        if include_google:
            print("\n=== Scanning Reddit via Google ===")
            google_results = self.google.search(keywords)
            print(f"  Found {len(google_results)} Reddit threads")
            all_results.extend(google_results)

        all_results.sort(key=lambda x: x.get("score", 0), reverse=True)
        return all_results

    def save_raw(self, results: list[dict], output_dir: str | Path = "data") -> Path:
        output_dir = Path(output_dir)
        output_dir.mkdir(parents=True, exist_ok=True)

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        filepath = output_dir / f"raw_scrape_{timestamp}.json"

        sources = {}
        for r in results:
            s = r.get("source", "unknown")
            sources[s] = sources.get(s, 0) + 1

        with open(filepath, "w", encoding="utf-8") as f:
            json.dump({
                "scraped_at": datetime.now(tz=timezone.utc).isoformat(),
                "total_posts": len(results),
                "sources": sources,
                "posts": results,
            }, f, indent=2, ensure_ascii=False)

        print(f"\nSaved {len(results)} posts to {filepath}")
        return filepath
