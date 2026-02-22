"""
Store builder — generates a dropshipping storefront via Lovable.
Constructs a detailed prompt from analyzed niches and sourced products,
then opens a Lovable Build URL to create the store.
"""

import json
import urllib.parse
import webbrowser
from datetime import datetime, timezone
from pathlib import Path


STORE_PROMPT_TEMPLATE = """\
Build a modern e-commerce dropshipping store called "{store_name}".

## Brand
- Store name: {store_name}
- Tagline: {tagline}
- Niche: {niche}
- Target audience: {target_audience}
- Brand vibe: clean, trustworthy, modern. Think minimal design with strong product photography.

## Pages Required
1. **Home page** — hero banner with tagline, featured products grid (3-6 products), \
trust badges (free shipping, 30-day returns, secure checkout), customer testimonials section.
2. **Product catalog** — filterable grid of all products with images, prices, \
"Add to Cart" buttons. Support category filtering.
3. **Product detail page** — large product images, description, price, quantity selector, \
Add to Cart, related products. Include a "Why customers love this" section.
4. **Cart** — slide-out or dedicated cart page showing items, quantities, subtotal, \
checkout button.
5. **About page** — brand story, mission, trust signals.
6. **Contact page** — contact form, email, FAQ section.

## Products to include:
{products_section}

## Design Requirements
- Mobile-first responsive design
- Color scheme: {color_scheme}
- Use Shadcn UI components
- Professional product card layout with hover effects
- Sticky header with cart icon showing item count
- Footer with newsletter signup, social links, quick navigation
- Loading states and smooth transitions
- Toast notifications for "Added to Cart"

## Technical
- Use React + TypeScript + Tailwind CSS + Shadcn UI
- Use Supabase for product data storage
- Include a simple admin view (password-protected) to manage products
- Products stored in Supabase with: name, description, price, images, category, stock status
- Cart state managed with React context
- Checkout page that collects shipping info (demo mode — no real payment processing)

## Important
This is a DEMO store for a hackathon. Include realistic-looking products with placeholder \
images (use Unsplash or placeholder services). The store should look production-ready even \
though payment processing is simulated."""


PRODUCT_ENTRY_TEMPLATE = """\
- **{name}**: ${sell_price:.2f} (was ${compare_price:.2f}) — {description}"""


COLOR_SCHEMES = {
    "Home": "warm neutrals — #F5F0EB background, #2C2C2C text, #C4A77D accents",
    "Kitchen": "clean whites — #FFFFFF background, #1A1A1A text, #4CAF50 accents",
    "Fitness": "bold energy — #1A1A2E background, #EAEAEA text, #E94560 accents",
    "Tech": "dark modern — #0D1117 background, #F0F6FC text, #58A6FF accents",
    "Beauty": "soft luxury — #FFF5F5 background, #4A4A4A text, #E8A0BF accents",
    "Pets": "friendly warm — #FFF8E7 background, #3E3E3E text, #FF8C42 accents",
    "Outdoor": "natural earth — #F0EDE5 background, #2D3436 text, #6AB04C accents",
}

STORE_NAME_TEMPLATES = {
    "Home": ["{word}Nest", "{word}Haven", "The {word} Co."],
    "Kitchen": ["{word}Kitchen", "Chef{word}", "The {word} Table"],
    "Fitness": ["{word}Fit", "Iron{word}", "{word}Athletics"],
    "Tech": ["{word}Tech", "Volt{word}", "{word}Labs"],
    "Beauty": ["{word}Glow", "Aura{word}", "{word}Beauty"],
    "Pets": ["{word}Paws", "Happy{word}", "The {word} Pet Co."],
    "Outdoor": ["{word}Trail", "Wild{word}", "{word}Outfitters"],
}


class StoreBuilder:
    """Generates Lovable prompts and launches store creation."""

    def generate_store_name(self, niche: dict) -> str:
        """Generate a brand name from the niche."""
        category = niche.get("category", "Home")
        niche_name = niche.get("name", "General")

        word = niche_name.split()[0] if niche_name else "Nova"
        templates = STORE_NAME_TEMPLATES.get(category, STORE_NAME_TEMPLATES["Home"])
        return templates[0].format(word=word)

    def generate_tagline(self, niche: dict) -> str:
        """Generate a tagline from niche data."""
        audience = niche.get("target_audience", "everyone")
        name = niche.get("name", "products")
        return f"Premium {name.lower()} for {audience.lower() if isinstance(audience, str) else 'modern living'}"

    def build_products_section(self, products: list[dict], niche_name: str = "") -> str:
        """Format sourced products into the prompt's product section."""
        if not products:
            return (f"- Include 6 realistic placeholder products that are specifically "
                    f"relevant to {niche_name or 'the niche above'}. "
                    f"Use realistic product names, prices ($15-$80 range), and descriptions.")

        lines = []
        for p in products[:8]:
            sell = p.get("sell_price", 29.99)
            compare = round(sell * 1.4, 2)
            lines.append(PRODUCT_ENTRY_TEMPLATE.format(
                name=p.get("name", "Product")[:100],
                sell_price=sell,
                compare_price=compare,
                description=p.get("niche", ""),
            ))

        if len(products) < 6:
            remaining = 6 - len(products)
            lines.append(f"\nAlso include {remaining} additional realistic products "
                         f"relevant to {niche_name or 'this niche'} with prices in the $15-$60 range.")

        lines.append(f"\nIMPORTANT: Only include products directly related to {niche_name or 'the niche'}. "
                     f"Do NOT include unrelated items.")
        return "\n".join(lines)

    def generate_prompt(self, niche: dict, products: list[dict]) -> str:
        """Generate the full Lovable store prompt."""
        category = niche.get("category", "Home")
        store_name = self.generate_store_name(niche)
        tagline = self.generate_tagline(niche)
        color_scheme = COLOR_SCHEMES.get(category, COLOR_SCHEMES["Home"])
        products_section = self.build_products_section(products, niche.get("name", ""))

        return STORE_PROMPT_TEMPLATE.format(
            store_name=store_name,
            tagline=tagline,
            niche=niche.get("name", "General"),
            target_audience=niche.get("target_audience", "general consumers"),
            products_section=products_section,
            color_scheme=color_scheme,
        )

    def build_lovable_url(self, prompt: str) -> str:
        """Construct a Lovable Build URL from the prompt."""
        encoded_prompt = urllib.parse.quote(prompt, safe="")
        return f"https://lovable.dev/?autosubmit=true#prompt={encoded_prompt}"

    def launch_store(
        self,
        niche: dict,
        products: list[dict],
        dry_run: bool = False,
    ) -> dict:
        """Generate prompt and launch Lovable to build the store."""
        prompt = self.generate_prompt(niche, products)
        url = self.build_lovable_url(prompt)
        store_name = self.generate_store_name(niche)

        result = {
            "store_name": store_name,
            "niche": niche.get("name", ""),
            "product_count": len(products),
            "prompt_length": len(prompt),
            "lovable_url": url,
            "created_at": datetime.now(tz=timezone.utc).isoformat(),
        }

        if dry_run:
            print(f"\n{'='*60}")
            print(f"DRY RUN — Store: {store_name}")
            print(f"{'='*60}")
            print(f"\nGenerated prompt ({len(prompt)} chars):\n")
            print(prompt)
            print(f"\n{'='*60}")
            print(f"Lovable URL ({len(url)} chars):")
            print(f"  (URL too long to display — would open in browser)")
            print(f"{'='*60}\n")
        else:
            print(f"\nLaunching Lovable to build '{store_name}'...")
            print(f"  Niche: {niche.get('name', 'Unknown')}")
            print(f"  Products: {len(products)}")
            print(f"  Prompt: {len(prompt)} characters")
            print(f"\n  Opening browser...")
            webbrowser.open(url)
            result["launched"] = True

        return result

    def save_store_config(self, result: dict, data_dir: str = "data") -> str:
        """Save the store configuration for reference."""
        data_path = Path(data_dir)
        data_path.mkdir(parents=True, exist_ok=True)

        stores_file = data_path / "stores.json"
        stores = []
        if stores_file.exists():
            with open(stores_file, "r", encoding="utf-8") as f:
                data = json.load(f)
                stores = data.get("stores", [])

        stores.append(result)

        with open(stores_file, "w", encoding="utf-8") as f:
            json.dump({
                "updated_at": datetime.now(tz=timezone.utc).isoformat(),
                "total": len(stores),
                "stores": stores,
            }, f, indent=2, ensure_ascii=False)

        return str(stores_file)
