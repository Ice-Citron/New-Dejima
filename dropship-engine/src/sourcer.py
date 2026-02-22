"""
Product sourcing via CJ Dropshipping API.
Searches for products, retrieves real supplier prices, and calculates margins.
"""

import json
import os
import time
import urllib.parse
import urllib.request
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path


CJ_API_BASE = "https://developers.cjdropshipping.com/api2.0/v1"
TOKEN_CACHE = Path(__file__).parent.parent / "data" / ".cj_token.json"


@dataclass
class ProductListing:
    name: str
    supplier_price: float
    shipping_cost: float
    sell_price: float
    source_url: str
    image_url: str
    supplier: str
    category: str
    cj_pid: str = ""

    @property
    def cogs(self) -> float:
        return self.supplier_price + self.shipping_cost

    @property
    def gross_margin(self) -> float:
        if self.sell_price <= 0:
            return 0
        return (self.sell_price - self.cogs) / self.sell_price

    @property
    def stripe_fee(self) -> float:
        return self.sell_price * 0.029 + 0.30

    @property
    def net_profit(self) -> float:
        """Profit per sale after COGS and Stripe fees (before ad spend)."""
        return self.sell_price - self.cogs - self.stripe_fee

    @property
    def net_margin(self) -> float:
        if self.sell_price <= 0:
            return 0
        return self.net_profit / self.sell_price

    def to_dict(self) -> dict:
        return {
            "name": self.name,
            "supplier_price": self.supplier_price,
            "shipping_cost": self.shipping_cost,
            "sell_price": self.sell_price,
            "cogs": round(self.cogs, 2),
            "gross_margin": f"{self.gross_margin:.0%}",
            "net_profit": round(self.net_profit, 2),
            "net_margin": f"{self.net_margin:.0%}",
            "stripe_fee": round(self.stripe_fee, 2),
            "source_url": self.source_url,
            "image_url": self.image_url,
            "supplier": self.supplier,
            "category": self.category,
            "cj_pid": self.cj_pid,
            "sourced_at": datetime.now(tz=timezone.utc).isoformat(),
        }


class CJClient:
    """CJ Dropshipping API client with token management."""

    def __init__(self, api_key: str | None = None):
        self.api_key = api_key or os.environ.get("CJ_API_KEY", "")
        self._access_token: str | None = None

    def _load_cached_token(self) -> str | None:
        if TOKEN_CACHE.exists():
            try:
                data = json.loads(TOKEN_CACHE.read_text(encoding="utf-8"))
                expiry = datetime.fromisoformat(data["expires_at"])
                if expiry > datetime.now(tz=timezone.utc):
                    return data["access_token"]
            except (json.JSONDecodeError, KeyError, ValueError):
                pass
        return None

    def _save_token(self, token: str, expires_at: str):
        TOKEN_CACHE.parent.mkdir(parents=True, exist_ok=True)
        TOKEN_CACHE.write_text(json.dumps({
            "access_token": token,
            "expires_at": expires_at,
            "cached_at": datetime.now(tz=timezone.utc).isoformat(),
        }), encoding="utf-8")

    def get_access_token(self) -> str:
        """Get a valid access token, using cache or requesting a new one."""
        if self._access_token:
            return self._access_token

        cached = self._load_cached_token()
        if cached:
            self._access_token = cached
            return cached

        if not self.api_key:
            raise ValueError("CJ_API_KEY not set. Add it to .env")

        url = f"{CJ_API_BASE}/authentication/getAccessToken"
        payload = json.dumps({"apiKey": self.api_key}).encode("utf-8")
        req = urllib.request.Request(
            url,
            data=payload,
            headers={"Content-Type": "application/json"},
            method="POST",
        )

        with urllib.request.urlopen(req, timeout=15) as resp:
            data = json.loads(resp.read().decode("utf-8"))

        if data.get("code") != 200 or not data.get("data"):
            raise ValueError(f"CJ auth failed: {data.get('message', 'Unknown error')}")

        token_data = data["data"]
        self._access_token = token_data["accessToken"]
        self._save_token(
            token_data["accessToken"],
            token_data["accessTokenExpiryDate"],
        )
        print(f"  CJ API authenticated (token valid for 15 days)")
        return self._access_token

    def _api_get(self, endpoint: str, params: dict | None = None) -> dict:
        """Make an authenticated GET request to the CJ API."""
        token = self.get_access_token()
        url = f"{CJ_API_BASE}{endpoint}"
        if params:
            query = urllib.parse.urlencode(params)
            url = f"{url}?{query}"

        req = urllib.request.Request(url, headers={"CJ-Access-Token": token})
        with urllib.request.urlopen(req, timeout=15) as resp:
            return json.loads(resp.read().decode("utf-8"))

    def search_products(self, keyword: str, page: int = 1, size: int = 20) -> list[dict]:
        """Search CJ product catalog."""
        data = self._api_get("/product/list", {
            "productNameEn": keyword,
            "pageNum": page,
            "pageSize": min(size, 200),
        })

        if data.get("code") != 200:
            print(f"    CJ search error: {data.get('message', 'Unknown')}")
            return []

        products = data.get("data", {}).get("list", [])
        return products

    def search_trending(self, page: int = 1, size: int = 20) -> list[dict]:
        """Get trending products from CJ."""
        data = self._api_get("/product/list", {
            "searchType": 2,
            "pageNum": page,
            "pageSize": min(size, 200),
        })

        if data.get("code") != 200:
            return []

        return data.get("data", {}).get("list", [])


class ProductSourcer:
    """Finds supplier listings via CJ Dropshipping and calculates real margins."""

    def __init__(self, cj_api_key: str | None = None):
        self.cj = CJClient(api_key=cj_api_key)

    def estimate_sell_price(self, supplier_price: float) -> float:
        """Estimate retail sell price using standard markup rules."""
        if supplier_price < 3:
            return 14.99
        elif supplier_price < 5:
            return 19.99
        elif supplier_price < 10:
            return 29.99
        elif supplier_price < 15:
            return 39.99
        elif supplier_price < 25:
            return 49.99
        elif supplier_price < 40:
            return 69.99
        else:
            return round(supplier_price * 2.0 / 10) * 10 + 9.99

    def _parse_price(self, price_val) -> float:
        """Parse CJ price which may be a number, string, or range like '3.02 -- 6.04'."""
        if isinstance(price_val, (int, float)):
            return float(price_val)
        if isinstance(price_val, str):
            if "--" in price_val:
                parts = price_val.split("--")
                return float(parts[0].strip())
            return float(price_val.strip())
        return 0.0

    NOISE_WORDS = {"for", "the", "with", "and", "set", "new", "hot", "products", "product",
                    "tools", "tool", "equipment", "gear", "accessories", "accessory", "items"}

    def _is_relevant(self, product_name: str, query: str, category_hint: str = "") -> bool:
        """Check if a product name is relevant to the search query."""
        if len(product_name) > 200:
            return False

        name_lower = product_name.lower()
        query_words = [w.lower() for w in query.split()
                       if len(w) > 2 and w.lower() not in self.NOISE_WORDS]

        if not query_words:
            return True

        matches = sum(1 for w in query_words if w in name_lower)

        # For short queries (1-2 meaningful words), require at least 1 match
        if len(query_words) <= 2:
            return matches >= 1

        # For longer queries, require at least 2 matches
        return matches >= 2

    def build_product_listing(self, cj_product: dict, category: str = "") -> ProductListing:
        """Convert a CJ API product into a ProductListing with margin calculations."""
        price = self._parse_price(cj_product.get("sellPrice", 0))
        if price <= 0:
            price = 1.0

        is_free_shipping = cj_product.get("isFreeShipping", False) or cj_product.get("addMarkStatus") == 1
        shipping = 0.0 if is_free_shipping else (2.0 if price < 10 else 3.50)

        name = cj_product.get("productNameEn", cj_product.get("productName", "Unknown"))
        pid = cj_product.get("pid", "")
        image = cj_product.get("productImage", "")
        cj_category = cj_product.get("categoryName", category)

        return ProductListing(
            name=name,
            supplier_price=price,
            shipping_cost=shipping,
            sell_price=self.estimate_sell_price(price),
            source_url=f"https://cjdropshipping.com/product/p-{pid}.html" if pid else "",
            image_url=image,
            supplier="cjdropshipping",
            category=cj_category or category,
            cj_pid=pid,
        )

    def source_niche(self, niche: dict) -> list[dict]:
        """Given a niche from the analyzer, find sourcing options via CJ."""
        name = niche.get("name", "")
        sample_products = niche.get("sample_products", [])
        category = niche.get("category", "")

        queries = [name]
        for product in sample_products:
            if isinstance(product, str):
                queries.append(product)
            elif isinstance(product, dict):
                queries.append(product.get("name", str(product)))

        all_listings = []
        seen_pids = set()

        for i, query in enumerate(queries[:5]):
            if i > 0:
                time.sleep(1.5)
            print(f"  Sourcing: '{query}'...")
            try:
                raw_products = self.cj.search_products(query, size=10)
                print(f"    Found {len(raw_products)} products on CJ")

                for cj_prod in raw_products:
                    pid = cj_prod.get("pid", "")
                    if pid in seen_pids:
                        continue
                    seen_pids.add(pid)

                    prod_name = cj_prod.get("productNameEn", "")
                    if not self._is_relevant(prod_name, query):
                        continue

                    listing = self.build_product_listing(cj_prod, category)
                    if listing.net_margin >= 0.30 and listing.supplier_price >= 1.0:
                        all_listings.append(listing.to_dict())

            except Exception as e:
                print(f"    CJ search failed for '{query}': {e}")

        return all_listings

    def get_trending(self, min_margin: float = 0.30) -> list[dict]:
        """Get CJ's trending products that meet margin requirements."""
        print("  Fetching CJ trending products...")
        try:
            raw = self.cj.search_trending(size=50)
            print(f"    Found {len(raw)} trending products")

            listings = []
            for cj_prod in raw:
                listing = self.build_product_listing(cj_prod)
                if listing.net_margin >= min_margin and listing.supplier_price >= 1.0:
                    listings.append(listing.to_dict())
            return listings
        except Exception as e:
            print(f"    CJ trending fetch failed: {e}")
            return []

    def source_from_niches(self, niches: list[dict], top_n: int = 5) -> list[dict]:
        """Source products for the top N niches via CJ Dropshipping."""
        all_products = []
        for niche in niches[:top_n]:
            print(f"\nSourcing niche: {niche.get('name', 'Unknown')}...")
            products = self.source_niche(niche)
            for p in products:
                p["niche"] = niche.get("name", "")
            all_products.extend(products)
            print(f"  {len(products)} profitable products found")

        all_products.sort(key=lambda x: x.get("net_profit", 0), reverse=True)
        return all_products
