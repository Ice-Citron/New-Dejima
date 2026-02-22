"""
Simple JSON-file storage for ideas and scrape history.
Keeps everything human-readable and git-friendly.
"""

import json
from datetime import datetime, timezone
from pathlib import Path


DATA_DIR = Path(__file__).parent.parent / "data"


class IdeaStore:
    def __init__(self, data_dir: Path | str | None = None):
        self.data_dir = Path(data_dir) if data_dir else DATA_DIR
        self.data_dir.mkdir(parents=True, exist_ok=True)
        self.ideas_file = self.data_dir / "ideas.json"
        self.ideas = self._load()

    def _load(self) -> list[dict]:
        if self.ideas_file.exists():
            with open(self.ideas_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                return data.get("ideas", []) if isinstance(data, dict) else data
        return []

    def _save(self):
        with open(self.ideas_file, "w", encoding="utf-8") as f:
            json.dump({
                "updated_at": datetime.now(tz=timezone.utc).isoformat(),
                "total": len(self.ideas),
                "ideas": self.ideas,
            }, f, indent=2, ensure_ascii=False)

    def add_ideas(self, new_ideas: list[dict]) -> int:
        """Add new ideas, deduplicating by name."""
        existing_names = {idea.get("name", "").lower() for idea in self.ideas}
        added = 0

        for idea in new_ideas:
            name = idea.get("name", "").lower()
            if name and name not in existing_names:
                idea["added_at"] = datetime.now(tz=timezone.utc).isoformat()
                idea["status"] = "new"
                self.ideas.append(idea)
                existing_names.add(name)
                added += 1

        self._save()
        return added

    def update_status(self, idea_name: str, status: str, notes: str = ""):
        """Update an idea's status: new -> investigating -> validating -> building -> rejected."""
        for idea in self.ideas:
            if idea.get("name", "").lower() == idea_name.lower():
                idea["status"] = status
                if notes:
                    idea.setdefault("notes", []).append({
                        "timestamp": datetime.now(tz=timezone.utc).isoformat(),
                        "note": notes,
                    })
                self._save()
                return True
        return False

    def get_by_status(self, status: str) -> list[dict]:
        return [i for i in self.ideas if i.get("status") == status]

    def get_top(self, n: int = 10) -> list[dict]:
        """Return top N ideas by confidence score."""
        scored = [i for i in self.ideas if i.get("confidence")]
        scored.sort(key=lambda x: x["confidence"], reverse=True)
        return scored[:n]

    def summary(self) -> dict:
        """Quick summary of stored ideas."""
        statuses = {}
        for idea in self.ideas:
            s = idea.get("status", "unknown")
            statuses[s] = statuses.get(s, 0) + 1

        return {
            "total": len(self.ideas),
            "by_status": statuses,
            "top_3": [
                {"name": i.get("name"), "confidence": i.get("confidence")}
                for i in self.get_top(3)
            ],
        }
