"""
JSON-file storage for niches, products, and store history.
Follows the same pattern as idea-engine's storage.
"""

import json
from datetime import datetime, timezone
from pathlib import Path


DATA_DIR = Path(__file__).parent.parent / "data"


class DropshipStore:
    """Tracks niches, sourced products, and store launches."""

    def __init__(self, data_dir: Path | str | None = None):
        self.data_dir = Path(data_dir) if data_dir else DATA_DIR
        self.data_dir.mkdir(parents=True, exist_ok=True)

        self.niches_file = self.data_dir / "niches.json"
        self.products_file = self.data_dir / "products.json"
        self.stores_file = self.data_dir / "stores.json"

        self.niches = self._load(self.niches_file, "niches")
        self.products = self._load(self.products_file, "products")
        self.stores = self._load(self.stores_file, "stores")

    def _load(self, filepath: Path, key: str) -> list[dict]:
        if filepath.exists():
            with open(filepath, "r", encoding="utf-8") as f:
                data = json.load(f)
                return data.get(key, []) if isinstance(data, dict) else data
        return []

    def _save(self, filepath: Path, key: str, items: list[dict]):
        with open(filepath, "w", encoding="utf-8") as f:
            json.dump({
                "updated_at": datetime.now(tz=timezone.utc).isoformat(),
                "total": len(items),
                key: items,
            }, f, indent=2, ensure_ascii=False)

    def add_niches(self, new_niches: list[dict]) -> int:
        """Add niches, deduplicating by name."""
        existing = {n.get("name", "").lower() for n in self.niches}
        added = 0
        for niche in new_niches:
            name = niche.get("name", "").lower()
            if name and name not in existing:
                niche["added_at"] = datetime.now(tz=timezone.utc).isoformat()
                niche["status"] = "new"
                self.niches.append(niche)
                existing.add(name)
                added += 1
        self._save(self.niches_file, "niches", self.niches)
        return added

    def add_products(self, new_products: list[dict]) -> int:
        """Add sourced products."""
        added = 0
        for product in new_products:
            product["added_at"] = datetime.now(tz=timezone.utc).isoformat()
            self.products.append(product)
            added += 1
        self._save(self.products_file, "products", self.products)
        return added

    def add_store(self, store: dict):
        """Record a store launch."""
        store["added_at"] = datetime.now(tz=timezone.utc).isoformat()
        self.stores.append(store)
        self._save(self.stores_file, "stores", self.stores)

    def get_top_niches(self, n: int = 5) -> list[dict]:
        """Return top N niches by confidence."""
        scored = [n for n in self.niches if n.get("confidence")]
        scored.sort(key=lambda x: x["confidence"], reverse=True)
        return scored[:n]

    def get_profitable_products(self, min_margin: float = 0.30) -> list[dict]:
        """Return products above minimum margin threshold."""
        result = []
        for p in self.products:
            margin_str = p.get("net_margin", "0%")
            try:
                margin = float(margin_str.strip("%")) / 100
            except (ValueError, AttributeError):
                margin = 0
            if margin >= min_margin:
                result.append(p)
        result.sort(key=lambda x: x.get("net_profit", 0), reverse=True)
        return result

    def summary(self) -> dict:
        """Quick summary of the dropship pipeline state."""
        top_niches = self.get_top_niches(3)
        profitable = self.get_profitable_products()

        return {
            "niches": {
                "total": len(self.niches),
                "top_3": [
                    {"name": n.get("name"), "confidence": n.get("confidence")}
                    for n in top_niches
                ],
            },
            "products": {
                "total": len(self.products),
                "profitable": len(profitable),
                "best": profitable[0] if profitable else None,
            },
            "stores": {
                "total": len(self.stores),
                "latest": self.stores[-1].get("store_name") if self.stores else None,
            },
        }
