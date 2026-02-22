# New Dejima

Autonomous app creation, tracked economics.

## Tech Stack

- **Framework:** Vue 3 + Vite
- **Language:** TypeScript
- **Styling:** Tailwind CSS v4
- **Animations:** @vueuse/motion
- **Icons:** Lucide
- **Routing:** Vue Router
- **State:** Pinia

## Setup

```bash
cd webapp
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173).

## Build

```bash
npm run build
npm run preview
```

## Structure

```
src/
  components/
    ui/         # Reusable design system (Button, Card, Modal, etc.)
    landing/    # Landing page sections
    app/        # App shell components (Sidebar, Charts, etc.)
  pages/        # Route-level page components
  layouts/      # Public + App layout wrappers
  router/       # Vue Router config
  stores/       # Pinia stores
  services/     # Mock data + API layer
  composables/  # Vue composables (scroll reveal, etc.)
  styles/       # Global CSS + design tokens
```
