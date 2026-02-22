"""
LLM-based niche and product analysis.
Uses Claude to evaluate product viability, competition, and margin potential.
"""

import json
import os
import re

from anthropic import Anthropic


NICHE_ANALYSIS_PROMPT = """\
You are an expert e-commerce analyst specializing in dropshipping.

Analyze the following scraped product trends and discussions. Extract the most \
promising PRODUCT NICHES for a new dropshipping store.

For each niche, provide:
- name: Short niche name (e.g. "Portable Blenders", "LED Strip Lights")
- category: Broad category (Home, Kitchen, Fitness, Tech, Beauty, Pets, Outdoor, etc.)
- description: 1-2 sentence description of the opportunity
- demand_signals: What evidence suggests demand? (trends, reddit buzz, search volume)
- competition_level: low / medium / high
- estimated_margin: Percentage margin after COGS and shipping (e.g. "40-60%")
- target_audience: Who buys this?
- risk_factors: What could go wrong?
- confidence: 1-10 rating of how promising this niche is
- sample_products: 3-5 specific product ideas within this niche

Return valid JSON array of niches. Extract at least 5, up to 15 niches.

Scraped data:
{data}"""


PRODUCT_EVAL_PROMPT = """\
You are an expert dropshipping product evaluator.

Evaluate this product idea for a dropshipping store:
{product}

Consider:
1. Demand: Is there proven demand? What evidence exists?
2. Competition: How saturated is this market? Can a new store compete?
3. Margins: What's realistic after COGS ($3-15 from AliExpress/CJ), shipping ($2-5), \
and platform fees (Stripe ~3%, ads ~30% of revenue)?
4. Sell price: What would customers pay? Check against existing stores.
5. Ad potential: Is this easy to market on social media? Visual appeal?
6. Shipping risk: Is it fragile? Heavy? Sizing issues that cause returns?
7. Seasonality: Is demand year-round or seasonal?

Return JSON:
{{
  "product": "name",
  "verdict": "GO" | "SKIP" | "MAYBE",
  "sell_price_estimate": "$XX",
  "cost_estimate": "$XX",
  "margin_estimate": "XX%",
  "demand_score": 1-10,
  "competition_score": 1-10,
  "risk_score": 1-10,
  "reasoning": "2-3 sentence explanation",
  "marketing_angle": "How to sell this on social media"
}}"""


class NicheAnalyzer:
    """Uses Claude to analyze scraped data and identify profitable niches."""

    def __init__(self, api_key: str | None = None):
        self.client = Anthropic(api_key=api_key or os.environ.get("ANTHROPIC_API_KEY"))

    def _call_llm(self, prompt: str) -> str:
        response = self.client.messages.create(
            model="claude-sonnet-4-20250514",
            max_tokens=4096,
            messages=[{"role": "user", "content": prompt}],
        )
        return response.content[0].text

    def _parse_json(self, text: str) -> list | dict:
        """Extract JSON from LLM response, handling markdown code fences."""
        cleaned = re.sub(r'```json\s*', '', text)
        cleaned = re.sub(r'```\s*$', '', cleaned.strip())
        try:
            return json.loads(cleaned)
        except json.JSONDecodeError:
            match = re.search(r'[\[{].*[}\]]', cleaned, re.DOTALL)
            if match:
                return json.loads(match.group())
            raise

    def analyze_niches(self, research_results: list[dict], batch_size: int = 20) -> list[dict]:
        """Analyze research results and extract promising niches."""
        condensed = []
        for r in research_results[:batch_size * 3]:
            entry = {"title": r.get("title", ""), "source": r.get("source", "")}
            if r.get("text"):
                entry["text"] = r["text"][:200]
            if r.get("score"):
                entry["score"] = r["score"]
            condensed.append(entry)

        batches = [condensed[i:i+batch_size] for i in range(0, len(condensed), batch_size)]
        all_niches = []

        for i, batch in enumerate(batches, 1):
            print(f"  Analyzing batch {i}/{len(batches)}...")
            prompt = NICHE_ANALYSIS_PROMPT.format(data=json.dumps(batch, indent=2))
            try:
                result = self._call_llm(prompt)
                niches = self._parse_json(result)
                if isinstance(niches, list):
                    all_niches.extend(niches)
                elif isinstance(niches, dict) and "niches" in niches:
                    all_niches.extend(niches["niches"])
            except Exception as e:
                print(f"  Batch {i} failed: {e}")

        return all_niches

    def evaluate_product(self, product_description: str) -> dict:
        """Quick-evaluate a single product idea."""
        prompt = PRODUCT_EVAL_PROMPT.format(product=product_description)
        result = self._call_llm(prompt)
        return self._parse_json(result)

    def rank_niches(self, niches: list[dict]) -> list[dict]:
        """Rank niches by confidence score, deduplicate similar ones."""
        seen = set()
        unique = []
        for niche in niches:
            name = niche.get("name", "").lower().strip()
            if name and name not in seen:
                seen.add(name)
                unique.append(niche)

        unique.sort(key=lambda x: x.get("confidence", 0), reverse=True)
        return unique
