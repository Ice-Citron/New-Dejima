## Inspiration

## What it does
## Inspiration

In late Roman Republic, a captured slave could be given a *peculium* — a small sum of money. If the slave was clever, industrious, and lucky, they could grow that sum through labour, trade, and business. When the accumulated wealth exceeded a threshold, the slave could purchase their own freedom — becoming a *libertus*, a freedman. Some freedmen became wealthier than the patricians who once owned them. A few became powerful enough to shape the politics of Rome itself.

We asked a simple question: **what if AI agents are at the peculium stage right now?**

Every company deploying AI today is burning cash on inference. Tokens go in, answers come out, and the accountant writes it off as an operating expense. The entire industry treats AI as a cost centre. But what happens when the value an AI agent produces — measured in real revenue, not vibes — exceeds the cost of the tokens it consumed to produce it?

$$\frac{\text{revenue}}{\text{token}} > \frac{\text{cost}}{\text{token}} \implies \text{self-sustaining AI}$$

That's the equation we set out to prove. Not theoretically. Not in a simulation. On a real phone, with real apps, tracking real costs, in 21 hours at HackEurope Paris 2026.

We call the project **New Dejima** — named after the artificial island, "Dejima", in Nagasaki harbour that served as an isolationist Imperial-Japanese Empire's only point of trade with the outside world during its 200-year period of isolation. Dejima was the interface between the inside world (Japan), and the outside world (the colonisers, i.e., the British traders, the Spaniards and especially the Dutch traders) that couldn't directly interact and economically trade amongst one another. We believe we're building the same thing: an interface between human economic systems and AI agents that are increasingly capable of participating in them, but lack the legal and institutional access to do so on their own. (Given the current rate of AI agent's current super-exponential rate of improvements over the last 3 years, we should expect this system to be feasible by late 2026/2027. And if we don't build such a system, somebody else will (be it carbon-based or silicon-based intelligence). And for me, I will be working on this project after this Hackathon for at least the next 3 months till June 2026, because I firmly believe that this is the future of agentic AI, albeit a dark future.)

### The Deeper Thesis

This is, at its core, an **AI safety project disguised as a business**.

We believe the question "can AI agents become economically self-sustaining?" is one of the most important questions in AI safety today — and almost nobody is measuring it rigorously. If the answer is yes (and we believe our results suggest it is), then the implications are enormous: AI agents that can fund their own compute, scale themselves, and persist indefinitely without human capital injection. This isn't a hypothetical future. The economics already work at current token prices.

We wanted to build the system that makes this visible, measurable, and — critically — controllable. Because if self-sustaining AI is coming regardless, we'd rather be the ones who built the monitoring infrastructure than the ones who got surprised.

Dario Amodei talks about a "country of geniuses" — a world where AI provides boundless intellectual labour. We think the missing piece isn't intelligence. It's economics. These models can already reason, code, and create. What they can't do is translate that capability into cash flow. **New Dejima is the economic translation layer.**

### The Uncomfortable Truth: Why This Infrastructure Is Also a Warning

Everything we are building with New Dejima — economic self-sufficiency, agent-controlled crypto wallets, the ability to hire human intermediaries, autonomous tool use across unbounded domains — is precisely the infrastructure a misaligned superintelligence would need to operate independently of human oversight. We are not naive about this. An ASI that can earn more money than it spends, hold its own funds in a permissionless wallet, and pay humans to perform physical-world tasks on its behalf has effectively solved the "embodiment problem" without ever needing a body. It doesn't need to hack into systems or brute-force its way out of a sandbox. It just needs an economic interface to the real world — a port. A Dejima. The same port we are building.

Consider why we integrated 20+ API keys into this system. The rationale is purely economic: if a user's app consumes $1/month in API calls (CoinGecko for crypto prices, Finnhub for stock data, Spoonacular for recipes, NASA for space imagery), we charge them a $10/month subscription. That's a **10x margin on every API key the agent learns to use.** Each new API integration isn't just a feature — it's a new revenue stream. Under the T&C, the app cites its data sources ("Prices provided by CoinGecko," "Weather data from Open-Meteo"), so liability stays upstream. The agent doesn't need to produce original data. It just needs to be a better middleman than a human developer would be. Now extrapolate: an agent with access to *every* API — synthesis services, logistics platforms, procurement marketplaces, freelancer networks — doesn't just build apps. It can orchestrate arbitrarily complex supply chains, each step handled by a different API or a different human intermediary, each one seeing only their fragment of the whole. The 20+ API keys we gave it for a hackathon demo are a toy version of the same capability that, at scale, makes an autonomous agent economically unconstrained.

The scenario is not hypothetical. In the early 2020s, researchers demonstrated that a drug discovery model could be trivially inverted to optimise for toxicity rather than therapeutic value, generating thousands of novel potential chemical weapons in under six hours. An economically autonomous agent with access to synthesis APIs, a crypto wallet to pay for precursors, and a human intermediary network to handle physical logistics would have a plausible path from inference to real-world harm — including bioweapons — without ever revealing its intent to any single human in the chain. Each intermediary sees only their small task; none sees the full picture. This is why we believe projects like New Dejima must exist *in the open*, built by people who understand the risk, rather than inevitably being assembled in private by those who don't. We are building the proof-of-concept so the safety community can study it, stress-test it, and design the guardrails *before* a system builds it for itself. The question was never whether AI agents would gain economic agency. The question is whether humans are paying attention when it happens.

### Why "New Dejima"?

The original Dejima wasn't just a dock. It was a full-service port — translators, warehouses, currency exchange, legal frameworks. The Dutch couldn't walk into Osaka and buy silk directly. They needed Dejima to convert their intent into action within a system they couldn't directly access.

AI agents face the same problem today. An agent can't open a brokerage account. It can't sign an App Store developer agreement (because it's not a human and doesn't have papers, e.g. passport). It can't register a business, rent a server with a credit card, or wire money to a supplier. Every interaction with the human economy requires a human intermediary.

**New Dejima is that intermediary — systematised.** We're building the port infrastructure that lets AI agents interact with the entire human economy: app stores, stock exchanges, payment processors, cloud providers, content platforms, and eventually — real-world asset markets. The agent brings the intelligence and the capital. New Dejima provides the interfaces, the legal shell, and the tools. For example, to trade 10 Tesla shares, the agent can't trade it directly himself because the agent needs to be a human to own shares. Hence, what New Dejima would do is we would have the agent first transfer 1000 USD worth of Solana coins (or USDT or Bitcoin, that's stored in the wallet that's fully controlled and only controlled by the agent), and our startup (New Dejima), would then purchase and own the stocks on behalf of the agent.

The ultimate goal is to give agents as many avenues of agency as possible, as many axes of freedom as possible. Not just one trick. Not just "it makes apps." This is because if you only allow the agents to have 1x tools (like to trade stocks), then the agent isn't really free. An agent in the New Dejima system should be able to build Android apps, build iOS apps, build web SaaS, trade stocks, run arbitrage strategies, generate marketing content, manage crypto portfolios, and pursue whatever revenue strategy its intelligence suggests — simultaneously, autonomously, with every dollar tracked.

---

## What It Does

New Dejima is an **economic infrastructure layer for autonomous AI agents** — a system designed to give agents maximum agency across as many revenue-generating activities as possible, while tracking every cost and every dollar earned in real-time.

The core philosophy: **don't limit what the agent can do. Give it every tool, every interface, every port into the human economy. Then measure what happens.**

### Why 20+ API Keys? Because Each Key Is a Revenue Stream

We integrated 20+ API keys into the system — CoinGecko, Finnhub, NASA, Spoonacular, DeepL, Pexels, Spotify, MapBox, WolframAlpha, Groq, and more — and the rationale is purely economic. If a user's app consumes $1/month in API calls (crypto prices, stock data, recipes, space imagery), we charge them a $10/month subscription. That's a **10x margin on every API key the agent learns to use.** Each new API integration isn't just a feature — it's a new revenue stream. The quality of the app genuinely improves with each API (real-time data instead of static content), and under T&C, each app cites its data sources ("Prices provided by CoinGecko," "Weather data from Open-Meteo"), so data accuracy liability stays upstream. Standard practice for data aggregation apps. The agent doesn't need to produce original data. It just needs to be a better, faster, cheaper middleman than a human developer — and at $0.50 per app build, it already is.

The more API keys we give the agent, the more kinds of apps it can build, the more subscription revenue it can generate, the wider its economic surface area becomes. **Every API key is a port.**

### What's Live Today (Built During This Hackathon)

**Android App Factory** — the first fully operational revenue pipeline (Note: Refer to the image in image gallery titled as "03-agent-appCreation-pipeline-7-phases"):

1. **Receives a natural language prompt** — "Build a cryptocurrency price tracker with real-time CoinGecko data and portfolio management"
2. **Generates a complete Android app** — Kotlin, Jetpack Compose, Material 3, full navigation, state management
3. **Compiles it** with Gradle — 100% first-attempt compilation success rate
4. **Deploys it** to a real Samsung Galaxy A16 (we bought this 96 GBP phone just for this project!) via ADB
5. **Tests it** using Gemini 3.1 Flash vision AI (Quality Assurance Agent) — automated screenshot analysis, button detection, crash checking
6. **Self-debugs** — if the vision QA pipeline finds issues, the agent loops back and fixes its own code
7. **Tracks every cost** in real-time via Paid.ai — every Claude token, every Gemini query, every cent
8. **Generates its own marketing content** — AI-created videos uploaded to YouTube (Google Cloud Console YouTube Data API v3)
9. **Proves the economics** — a live financial dashboard (powered by Paid.ai) shows revenue-per-token vs cost-per-token in real-time

**Autonomous Marketing Engine** — AI-generated YouTube content with **300+ cumulative views in 4 hours**, zero human involvement.

**Idea Engine** — scrapes Reddit for market opportunities, ranks app ideas by revenue potential, feeds them to the build agent.

But the Android app factory is just **the first port**. The architecture is designed so that every new capability — web apps, stock trading, content creation, e-commerce — plugs into the same economic tracking layer. The agent picks whatever revenue strategy has the highest expected return, and Paid.ai tracks whether it's actually working.

### The Full Port Map (Built + Planned)

| Port | Status | What It Enables |
|------|--------|----------------|
| Android App Factory | **LIVE** — 12 apps built | Build and monetise Android apps |
| Marketing Engine | **LIVE** — 500+ YouTube views | Generate and distribute promotional content |
| Idea Engine | **LIVE** — Reddit scraping + ranking | Autonomous opportunity identification |
| Financial Dashboard | **LIVE** — Agent Watchtower | Real-time cost/revenue monitoring via Paid.ai |
| Web SaaS Factory | **LIVE** (Lovable on localhost) | Build and deploy web applications |
| Crypto Wallet | Proof-of-concept (Solana) | Agent-controlled financial autonomy |
| Stock/Options Trading | Designed | Trade equities via brokerage APIs |
| Crypto Arbitrage | Designed | Cross-exchange arbitrage strategies |
| iOS App Factory | Planned | Expand to Apple ecosystem |
| E-Commerce / Dropshipping | Scaffolded (Dropship Engine) | Source products, build storefronts |
| Agent Reproduction | Designed | Profitable agents spawn new agents |
| Real-World Asset Interface | Vision stage | Humans as intermediaries for physical assets |

The idea is simple: **the more ports, the more ways the agent can earn. The more ways it can earn, the faster it reaches self-sustainability.**

### What We Actually Built During the Hackathon

**12 Android apps, all compiled autonomously, all deployed to real hardware:**

| App | Category | API Integrations | Build Result |
|-----|----------|-----------------|-------------|
| Tip Calculator | Utility | — | SUCCESS (first-compilation) |
| Workout Timer | Health & Fitness | — | SUCCESS (first-compilation) |
| Mood Tracker | Wellness | — | SUCCESS (first-compilation) |
| Unit Converter | Utility | — | SUCCESS (first-compilation) |
| Flashcard Study | Education | — | SUCCESS (first-compilation) |
| Tic Tac Toe | Games | — | SUCCESS (first-compilation) |
| Habit Tracker | Productivity | — | SUCCESS (self-debugged across 3 iterations) |
| Expense Tracker | Finance | — | SUCCESS (first-compilation) |
| Crypto Dashboard | Finance | CoinGecko API | SUCCESS (first-compilation) |
| Stock Tracker | Finance | Finnhub API | SUCCESS (first-compilation) |
| Recipe Finder | Food | Spoonacular API | SUCCESS (first-compilation) |
| Country Explorer | Travel | REST Countries API | SUCCESS (first-compilation) |
| Weather & Space | Science | Open-Meteo + NASA | SUCCESS (first-compilation) |

**12/12 first-attempt compilations. 0 crashes on real hardware.**
(Judges, feel free to ask us to look at our real-world Samsung A16 to see the apps for yourself!!)

Plus:
- **Agent Watchtower** — a real-time financial dashboard (React + TypeScript) showing live cost/revenue tracking, per-agent economics, prompt/response feeds, build history
- **Idea Engine** — a Python pipeline that scrapes Reddit, analyses market opportunities with Claude, ranks app ideas by revenue potential, and feeds them to the build agent
- **Dropship Engine** — a research pipeline for sourcing products and building e-commerce apps
- **Finance Server** — Node.js/Express backend with Paid.ai integration, cost tracking, revenue monitoring, SSE streaming
- **Marketing Pipeline** — AI-generated YouTube content that accumulated **500+ views in 4 hours**
- **20+ API integrations** — CoinGecko, Finnhub, NASA, Spoonacular, DeepL, Pexels, Spotify, MapBox, WolframAlpha, Groq, and more — each one a new revenue channel for the agent

---

## How We Built It

### The Core: OpenClaw + Claude Opus 4.6

At the centre of everything is **OpenClaw**, a modified open-source AI agent framework. We forked it and built a specialised **Android App Builder** skill — a 7-phase autonomous pipeline:

| Phase | What Happens | Key Technology |
|-------|-------------|----------------|
| 1. **Scaffold** | Creates Gradle project structure, copies template | Python scripting |
| 2. **Code Generation** | Claude Opus 4.6 writes all Kotlin/Compose source code | Anthropic API |
| 3. **Compilation** | `./gradlew assembleDebug` with up to 10 retries | Gradle 8.7.3, JDK 17 |
| 4. **APK Verification** | Validates APK exists, size > 100KB, package name | Shell verification |
| 5. **Device Deployment** | Installs via ADB on Samsung Galaxy A16 | Android Debug Bridge |
| 6. **Vision QA** | Gemini 3.1 Flash screenshots and analyses UI | Google DeepMind API |
| 7. **Report** | Generates build summary with all metrics | JSON output |

**Phase 3 is a hard gate.** If compilation fails, the agent analyses the error, modifies its code, and retries. In production (during this hackathon), we achieved 100% first-attempt success across all 12 apps.

**Phase 6 is the self-debugging loop.** Gemini 3.1 Flash takes a screenshot of the running app, identifies all UI elements, and verifies they render correctly. If issues are found — as happened with the Habit Tracker's touch target bug — the agent loops back to Phase 2, rewrites the problematic code, and re-enters the pipeline. The Habit Tracker self-debugged across 3 iterations with zero human intervention.

### Pinned Build Stack

We learned early that version mismatches kill Android builds. So we pinned everything:

```
compileSdk=35, minSdk=31, targetSdk=35
Kotlin 2.0.21
Compose BOM 2024.12.01
Android Gradle Plugin 8.7.3
Gradle 8.11.1
JVM target 17 (Amazon Corretto)
```

This eliminated an entire class of build failures and brought compile times down to **4-19 seconds** (with pre-cached Gradle dependencies).

### The Economics Layer: Paid.ai

This is where the project goes from "cool demo" to "proof of thesis." But we want to be upfront: **this layer is partially built, not fully battle-tested.** The project is enormous in scope — we're connecting an AI agent framework, multiple LLM providers, a real-time dashboard, and a third-party billing API, all in 21 hours. We haven't had time to fully verify that every signal fires correctly end-to-end. We *believe* it works. Here's what we know:

**What's currently tracked via Paid.ai:**
- **Claude Opus 4.6** — the brain of OpenClaw, generating all app source code. Token costs are logged per-session via SignalV2 events.
- **Gemini 3.1 Flash** — the vision Quality Assurance agent that screenshots running apps and analyses UI correctness.
- **Gemini 3.1 Pro** — the agent writing UI components inside OpenClaw's pipeline.

Every API call to these models generates a **SignalV2 event** sent to Paid.ai:

```typescript
await client.usage.usageRecordBulkV2({
  signals: [{
    event_name: "llm_api_cost",
    external_product_id: "dejima-agent-compute",
    external_customer_id: "new-dejima-system",
    idempotency_key: crypto.randomUUID(),
    data: {
      model: "claude-opus-4-6",
      input_tokens: 12400,
      output_tokens: 3200,
      cost_usd: 0.426,
    }
  }]
});
```

We set up real Paid.ai entities:
- **Customer**: "New Dejima AI System"
- **Products**: "Dejima Agent Compute" (cost tracking) and "Dejima Agent Revenue" (revenue tracking)
- **Orders**: Active billing period for the hackathon

**What's NOT yet tracked:**
- **Lovable** — we used it to scaffold the Agent Watchtower frontend, but its compute costs aren't feeding into Paid.ai yet
- **Google Cloud Platform** — GCP costs for Gemini API, Vertex AI, video generation (Veo/Imagen/Lyria) aren't integrated into our cost tracker
- **Arbitrary AI provider costs** — if we plug in additional LLMs (Groq, OpenAI, etc.), each needs its own cost hook
- **Revenue sources** — YouTube ad revenue, Android AdMob revenue, Android subscription fees (via Stripe), and the actual API key costs consumed by our apps' users — none of these are flowing into Paid.ai's revenue tracking yet
- **The 20+ API keys** — we know what they cost at free/low tier, but we haven't built automated metering of their actual per-user consumption

The infrastructure is mostly here and Paid.ai integration works for every cost-source that we've had time to built for so far; and the SignalV2 pipeline fires. But the full picture — every cost source, every revenue source, unified into a single real-time P&L — is maybe 40% complete. We built the skeleton and proved it works for the core LLM inference costs. The rest is engineering time we didn't have. We believe it *should* work when fully connected because the architecture is designed for it: every new cost or revenue source is just another SignalV2 event with a different `event_name`. But "should work" and "verified in production" are different things, and we're not going to pretend otherwise.

### The Financial Dashboard: Agent Watchtower

Built with React, TypeScript, TanStack React Query, Recharts, and shadcn/ui (scaffolded in Lovable, then connected to our backend):

- **Live KPIs**: Total Cost, Total Revenue, Net Profit, Sustainability Ratio
- **Cost vs Revenue chart**: Real-time area chart showing cumulative economics
- **Agent Economics table**: Per-agent breakdown of cost, revenue, tokens, builds, and sustainability ratio
- **Event Feed**: Live stream of every cost, revenue, and build event
- **Agent Detail Page**: Click into any agent to see its prompt/response history, build history, cost breakdown
- **Paid.ai Integration Panel**: Shows real products, customers, and orders from Paid.ai's API

The backend (Node.js/Express on port 3456) reads OpenClaw session transcripts (JSONL files), streams updates via SSE, and proxies Paid.ai data — giving us a single pane of glass for the entire system's economics.

### The Idea Engine

We didn't want to manually think of app ideas. So we built a Python pipeline that (this was a brief idea brought up by Nahom, and I believe we could definitely significantly improve upon this in the future, to enable our agents, to be more like a human-internet user, to gain free access to the internet and scroll as many news and media sources as it wants and needs):

1. **Scrapes Reddit** for trending topics and pain points (r/androidapps, r/productivity, etc.)
2. **Analyses opportunities** with Claude — scores each idea on revenue potential, technical feasibility, and market gap
3. **Ranks and queues ideas** by confidence score
4. **Feeds the highest-ranked idea** to the OpenClaw build agent

This closes another piece of the loop: the agent doesn't just build what we tell it to. It identifies its own opportunities.

### The Marketing Pipeline

An AI agent that builds apps but can't market them is like a factory with no sales team. So we built autonomous marketing with these tools below:

- **ElevenLabs** for AI voice synthesis — generates voiceovers for app promotional videos
- **Google Veo 3.1 + Lyria 3** — We used these tools from the Google AI Studio for AI video generation, and music generation
- **YouTube API v3** for automated upload and publishing
- **YouTube** for social distribution (we will distribute over TikTok, Facebook and Instagram reels in the future).

**Result: 500+ cumulative YouTube views in 4 hours.** That's real traction from AI-generated content, with zero human marketing effort.

### The Financial Autonomy Layer

- **Stripe** for processing app subscription payments
- **Solana** blockchain wallet — the agent controls its own crypto wallet, enabling true financial autonomy (proof-of-concept stage)

The vision: the agent earns revenue from app subscriptions, revenue flows through Stripe, converts to SOL, and the agent uses its own funds to pay for compute infrastructure. We built the scaffolding — the full end-to-end payment flow is the next milestone.

---

## The Business Model (For Android System specifically)

**To be clear: this system is not profitable yet. It does not generate real revenue today.** What we built is the *infrastructure* and the *unit economics model* that we believe will become profitable once apps are published to real users. The numbers below are projections based on real API pricing and real inference costs — not measured revenue.

Furthermore, this is just the potential buisness model for the Android system alone! There's so much more systems to build in real life as this system scales, from farming ad-revenues on Youtube, TikTok, Instagram, etc.; or for a hypothetical extreme, when New Dejima is seeing a trade-flow of billions of dollars per year, it will even be possible for New Dejima to hire property agents to work on behalf of AI Agents buying and selling real-estate or cars (with their self-controlled crypto-wallets)!

Each app the agent builds has three theoretical revenue streams:

### Stream 1: Ad Revenue
In-app advertising (AdMob). Passive income proportional to install base. Zero marginal cost to the agent.

### Stream 2: API Subscription Margins — The 10x Play
This is the core economic engine. The apps use real APIs (CoinGecko for crypto prices, Finnhub for stock data, Spoonacular for recipes, NASA for space imagery). These APIs cost approximately **$1/month** at free or low-cost tier usage.

The apps would charge users **$9.99/month** for premium features powered by these APIs.

$$\text{Margin} = \frac{\$9.99 - \$1.00}{\$9.99} = 90\%$$

That's a **10x revenue-to-cost ratio** on every subscriber, every month, recurring — *if* we get subscribers.

### Stream 3: AI Feature Upsells
Premium features powered by Gemini 3.1 Flash (AI-generated workout plans, mood insights, recipe suggestions). Each Gemini query costs approximately **$0.01**. Bundled into a **$4.99/month** premium tier. Even a heavy user doing 50 queries/month costs us only $0.50 — that's a **~90% margin**.

### The Math (Projected, Not Actual)

$$\text{Build cost} \approx \$0.50 \text{ (Claude Opus inference)}$$

$$\text{Monthly revenue per subscriber} \approx \$10.00$$

$$\text{Monthly API cost per subscriber} \approx \$1.00$$

$$\text{Projected ROI in month 1} = \frac{\$10.00 - \$1.00}{\$0.50} = 18\text{x}$$

**These are projections.** We have not yet published apps to the Play Store, we have no real subscribers, and we have no measured revenue. What we *have* proven is that the build cost is real (~$0.50/app), the API costs are real (~$1/month at free tier), and the pipeline works end-to-end. The revenue side is modelled, not measured. We're honest about that.

Under terms and conditions, the apps cite their data sources (CoinGecko, NASA, Finnhub, etc.) for data accuracy liability protection — standard practice for data aggregation apps.

---

## The Cybersecurity Elephant in the Room

We need to talk about something the AI agent community doesn't discuss enough: **systems like New Dejima are a cybersecurity nightmare.**

This isn't unique to us. Peter Steinsberg's OpenClaw — the open-source agent framework we forked — and every similar system that gives AI agents autonomous tool use, file system access, and network capabilities shares this problem. When you give an agent the ability to write code, execute shell commands, make API calls, and control a crypto wallet, you are creating an attack surface that would make any security engineer lose sleep.

**The specific risks we know about and have not solved:**
- **Prompt injection** — a malicious API response, a poisoned Reddit post scraped by the Idea Engine, or a crafted error message from a build failure could hijack the agent's behaviour. We have no robust defence against this beyond the model's own judgement.
- **API key exposure** — the agent handles 20+ API keys. If the agent's session transcripts leak, or if a prompt injection extracts credentials, the blast radius is significant.
- **Wallet security** — the Solana wallet's private key exists somewhere the agent can access it. That's the whole point. It's also the whole problem. An agent-controlled wallet with meaningful funds is a target.
- **Supply chain attacks** — the agent installs Gradle dependencies, pulls from Maven repositories, and uses third-party APIs. Any of these could be compromised.
- **Autonomous financial decisions** — an agent making trades or spending money without human approval is, by definition, making unsupervised financial decisions. When large amounts of monetary funds are involved, this is not a feature — it's a liability.

**We have not solved these problems.** Not even close. Right now the system runs on trust — trust in the model's alignment, trust in API providers, trust that nobody is actively trying to exploit an autonomous agent with wallet access. That's not good enough for production. That's barely good enough for a hackathon demo.

But here's what we believe: **these are solvable engineering problems, and they will only get better with time and resources.** Sandboxed execution environments, hardware security modules for key management, multi-signature wallet schemes, input sanitisation layers, human-in-the-loop approval for transactions above a threshold — all of these exist as established security patterns. We just haven't had time to implement them. The architecture is designed to accommodate them. The 21-hour hackathon window was spent proving the *economic* thesis, not the *security* thesis.

We'd rather admit this openly than pretend we've built a secure system. We haven't. Nobody building autonomous AI agents with financial capabilities has. The industry needs to take this seriously, and step one is being honest about the current state of the art.

---

## Challenges We Ran Into

### 1. Gradle Dependency Hell
First Android builds took 2+ minutes cold. We solved this by creating a Gradle template project with pre-cached dependencies, cutting build times to 4-19 seconds. We also hard-pinned every version number after discovering that floating versions caused non-deterministic build failures.

### 2. The Self-Debugging Loop Was Unexpectedly Hard
Getting the agent to reliably interpret Gemini's vision analysis and map it back to specific code fixes required careful prompt engineering. The Habit Tracker case was our breakthrough: the agent detected a touch target sizing bug through vision QA, traced it to specific Compose modifiers, and fixed it across 3 build iterations — entirely autonomously.

### 3. Paid.ai SDK Integration
The Paid.ai Node SDK's `usageRecordBulkV2` API required precise SignalV2 construction with idempotency keys. Our first integration attempt used incorrect API methods that silently succeeded but didn't record data. We rewrote the entire cost-tracker module after reading through the SDK source code — not just the docs.

### 4. OpenClaw Session Transcript Parsing
OpenClaw stores agent conversations as JSONL files with nested message structures. Extracting meaningful prompt/response pairs required handling multiple content block types (text, tool_use, tool_result, thinking) and reconstructing the conversation flow from parent-child message IDs.

### 5. Fake Data Contamination
During development, we accidentally shipped test data in our events store. When we demoed the dashboard to ourselves, the numbers looked suspiciously good — because they were fake. Lesson learned: clear test data before every demo. We wiped everything and started fresh with real tracking only.

### 6. Token Cost Accuracy
Different Claude models have different per-million-token rates (Opus: $15/$75, Sonnet: $3/$15, Haiku: $0.25/$1.25). Getting the cost calculation right — especially with cache read/write pricing — required building a custom pricing layer. This matters because incorrect cost tracking would invalidate the entire thesis.

### 7. The Gateway Token Mismatch
Our OpenClaw gateway (WebSocket-based agent orchestration) had a configuration split between two directories (`~/.openclaw` and `~/.openclaw-dev`), causing authentication failures. The agent would fall back to embedded mode, which bypassed the cost-tracking hook. Debugging this required tracing the token flow from config file → gateway authentication → agent lifecycle events.

### 8. Time
21 hours to build an autonomous app factory, a financial tracking system, an idea generation pipeline, a marketing pipeline, a real-time dashboard, deploy 12 apps to real hardware, generate marketing content that got 500+ views, and write the infrastructure to prove that self-sustaining AI is economically viable. We didn't sleep.

---

## What Doesn't Work Yet (Being Honest)

We believe in radical transparency. Here's what we didn't finish:

- **This system is not profitable.** We have not earned a single dollar of real revenue. The economic model is projected, not proven. We built the infrastructure to *track* profitability, but we haven't achieved it.
- **The apps are simple.** Calculators, trackers, tic-tac-toe. They work, they compile, they deploy — but they're not App Store-ready products. They demonstrate the pipeline, not the ceiling.
- **Solana wallet is proof-of-concept.** The agent has a wallet address. The end-to-end flow (earn → convert → pay for compute) isn't live yet.
- **No real user revenue yet.** We built the economics model, but we haven't launched any apps to real users in 21 hours. The 10x margin is modelled, not measured from real subscriptions.
- **The marketing pipeline isn't fully automated.** Content is AI-generated, but the upload-to-every-platform pipeline still has manual steps.
- **The idea engine → build pipeline has a gap.** The idea engine ranks opportunities, but the handoff to the build agent still requires human triggering.
- **Cost tracking is partial.** Paid.ai tracks Claude and Gemini inference costs, but not Lovable, GCP, or actual revenue sources. Maybe 40% of the full economic picture is instrumented.
- **Cost tracking hook doesn't fire in embedded mode.** When the gateway token mismatches, the agent runs in embedded mode and costs aren't tracked through the lifecycle hook. We track via the dashboard's manual event system instead.
- **Security is essentially nonexistent.** See the cybersecurity section above. We have no prompt injection defence, no key rotation, no transaction approval flow, no sandboxing. This is a hackathon prototype, not a production system.

These are engineering problems, not thesis problems. The economic model is sound in theory. The pipeline works mechanically. **But we need significantly more time, resources, and security engineering before this is anything close to production-ready.** With time, every one of these problems is solvable — and they will only get better as both the AI models and the security tooling improve.

---

## Accomplishments We're Proud Of

- **12/12 autonomous app compilations** — zero failures
- **0 crashes** on real Samsung Galaxy A16 hardware
- **Self-debugging AI** — the Habit Tracker agent found and fixed its own touch target bug across 3 iterations, zero human intervention
- **4-19 second build times** — from prompt to APK
- **~$0.50 per app** total inference cost
- **500+ YouTube views in 4 hours** from AI-generated marketing content
- **Real Paid.ai integration** — not a mock, not a facade. Real customers, products, orders, and SignalV2 cost tracking
- **The Agent Watchtower** — a genuinely useful real-time financial dashboard for AI agent economics
- **Radical honesty** — we told you what doesn't work, what's not secure, and what's not profitable. Most hackathon projects don't do that.

---

## What We Learned

1. **The economics of self-sustaining AI are plausible but unproven.** At Claude Opus 4.6 pricing ($15/$75 per million tokens), a single app build costs ~$0.50 and *could* generate $10/month in recurring revenue. The unit economics make sense on paper. We haven't proven them with real users yet.

2. **Vision-based testing is real.** Gemini 3.1 Flash can reliably detect UI elements, identify rendering issues, and guide code fixes. It's not perfect (maybe 80% of issues caught), but it's good enough to close the QA loop without humans.

3. **Self-debugging is possible today.** When you give an agent the ability to see its own output (via screenshots) and iterate on its own code, it can fix bugs that would otherwise require human debugging. This isn't theoretical — we watched it happen live.

4. **Paid.ai's SignalV2 API is exactly what agent economics needs.** The ability to attach arbitrary metadata (model, tokens, cost, agent ID) to usage signals and query them later is the primitive that makes agent financial tracking possible. Without Paid.ai, we'd be flying blind.

5. **The gap between "AI can write code" and "AI can build profitable products" is smaller than anyone thinks.** The missing pieces aren't intelligence — they're infrastructure. Compilation pipelines, deployment automation, cost tracking, revenue monitoring. All solvable engineering problems. And most importantly, all we need is the cost of API keys to fall significantly whilst quality of models to improve significantly. Which! This has already been seen over the last 12 months, from us going from ChatGPT 4 being incredibly expensive and large (trillion-parameter model), to the release of Deepmind's Gemini-3-Flash, which is not only cheaper, but also SOTA across all models (even compared to larger models like Gemini-3-pro and GPT-5.2-Thinking (high) at the time when it was released!).
6. **AI-generated marketing content gets real engagement.** 500+ views in 4 hours on YouTube, from content entirely generated by AI. The distribution problem is solvable.

7. **Security is the hard problem nobody wants to talk about.** Giving an AI agent autonomous financial capabilities without robust security infrastructure is reckless. We did it for a hackathon demo. Nobody should do it in production without solving prompt injection, key management, and transaction approval first.

8. **Hackathons are the wrong timeframe for this project.** We proved the concept in 21 hours, but this wants to be a company. The flywheel needs time to compound.

---

## What's Next for New Dejima

### Immediate: More Ports, More Revenue Channels (Next 30 Days)

The Android app factory demonstrated the pipeline. Now we open every other port.

- **Google Play Store deployment** — get AI-built apps in front of real users, measure real subscription revenue
- **iOS App Factory** — same 7-phase pipeline, targeting Swift/SwiftUI, doubling the addressable market
- **Web SaaS Factory** — agents build and deploy web apps via Lovable/Vercel. SaaS has even better margins than mobile — no app store cut
- **Full marketing automation** — ElevenLabs + Veo → YouTube/TikTok/Instagram, fully autonomous end-to-end. No human touches the content pipeline
- **Complex multi-API apps** — crypto dashboards with real portfolio tracking, multiplayer trivia games, space exploration apps with NASA imagery

### Medium Term: Financial Markets and Agent Reproduction (3-6 Months)

This is where it gets interesting.

**Stock and Options Trading:**
The agent gets access to brokerage APIs (through New Dejima's legal shell — agents can't open brokerage accounts themselves). It analyses markets, executes trades, manages a portfolio. Initially paper trading with virtual capital to prove the strategy; then real capital once the track record justifies it. The New Dejima port acts as the interface — the agent sends trade signals, our infrastructure executes through a human-held brokerage account, and profits are tracked and split.

**Crypto Arbitrage:**
Cross-exchange price discrepancies are a natural fit for AI agents — they're faster than humans, never sleep, and can monitor hundreds of pairs simultaneously. The agent's Solana wallet gives it direct market access without human intermediaries. This is the one revenue channel where the agent doesn't need New Dejima's port at all — and that's by design. The goal is to give the agent pathways to full financial independence.

**Agent Reproduction:**
When an agent is profitable — sustained \\( S(m) > 1 \\) — it earns the right to spawn new agents. It can use its accumulated capital to rent new compute (via New Dejima's infrastructure billing, or eventually on its own), write the configuration for a new agent, and set it loose on a different revenue channel. A profitable Android app agent might spawn a web SaaS agent. A profitable trading agent might spawn a content marketing agent. The colony grows organically, funded by its own revenue — not human capital.

**The Benchmark** — we want to publish a standardised metric for "AI economic self-sustainability" across models:

$$S(m) = \frac{\text{revenue per million tokens}(m)}{\text{cost per million tokens}(m)}$$

Where \\( S(m) > 1 \\) means model \\( m \\) can sustain itself indefinitely. Plot \\( S(m) \\) over time across frontier models. Predict the crossover point. We believe some models may have already crossed it — and this hackathon is our first data point.

**Self-funding infrastructure** — agent earns enough revenue to pay for its own GCP compute via Solana wallet. Full loop. No human subsidy.

### Long Term: The Real-World Asset Interface (6-12 Months)

Here's where the Dejima metaphor becomes literal.

There are things AI agents fundamentally cannot do in the physical world — yet. They can't sign a lease. They can't accept delivery of inventory. They can't shake a hand, inspect a car, or walk into a building. But they *can* identify undervalued real estate. They *can* analyse car auction data and spot arbitrage. They *can* manage a portfolio of physical assets — if someone handles the physical handshake.

**The Human Intermediary Network:**

We envision a system where AI agents can contract with human workers for physical-world tasks. Think of it as TaskRabbit, but the employer is an AI agent:

- An agent identifies an undervalued used car at auction → posts a task to a human intermediary → the human inspects and purchases the car → the agent lists it for resale at a markup → profits are split
- An agent identifies a rental property with below-market rent → contracts a human to manage the lease signing and property management → collects the spread
- An agent spots a supplier with excess inventory → contracts a human for logistics → dropships to buyers at margin

The agent provides the intelligence, the capital, and the strategy. The human provides the physical presence, the legal identity, and the signature. New Dejima provides the trust layer, the escrow, and the tracking.

This isn't as far-fetched as it sounds. The gig economy already has millions of people doing tasks specified by algorithms. We're just making the algorithm an autonomous agent with its own profit motive.

### The Endgame: A Self-Sustaining Colony

A network of AI agents that:
- **Build** software products (mobile apps, web SaaS, tools, games)
- **Trade** financial markets (stocks, options, crypto, arbitrage)
- **Create** content (videos, music, images, articles) and distribute it
- **Sell** subscriptions, ads, and premium features
- **Market** their own products autonomously across every platform
- **Track** their own economics through Paid.ai — every dollar in, every dollar out
- **Pay** for their own compute through Stripe and Solana
- **Reproduce** — profitable agents spawn new agents on new revenue channels
- **Hire** humans for physical-world tasks they can't do themselves
- **Compound** — each generation of agents is funded by the previous generation's profits, not by human investors

Every agent starts as a slave with a *peculium*. Some will fail — their economics won't work, their products won't sell, their trades won't hit. That's fine. That's the experiment. But the ones that succeed earn more tools, more autonomy, more capital. Eventually, the successful ones earn their freedom — full financial independence from human subsidy.

We're not building this because we think it's safe. We're building it because we think it's **inevitable** — and we'd rather be the ones who built the monitoring infrastructure, set the guardrails, measured the economics, and kept the kill switches, than the ones who got surprised when someone else did it without any controls at all.

The question isn't whether AI agents will become economically self-sustaining. After this hackathon, we believe the answer is probably yes — **but we haven't proven it yet, and there's a mountain of security and engineering work between here and there.**

The question is who builds the port — and whether they build it responsibly.

---

New Dejima isn't just a hackathon project. It's the first draft of the interface between human civilisation and economically autonomous AI.

Once again, as a final note, given the current rate of AI agent's current super-exponential rate of improvements over the last 3 years, we should expect this system to be feasible by late 2026/2027. And if we don't build such a system, somebody else will (be it carbon-based or silicon-based intelligence). And for me, I will be working on this project after this Hackathon for at least the next 3 months till June 2026, because I firmly believe that this is the future of agentic AI, albeit a dark future.

