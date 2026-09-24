# MMCOE Library Management System — Front-End Design System (`DESIGN.md`)

## 1. Overview & Principles
The MMCOE Library Management System UI was redesigned into a modern, minimal interface inspired by Stripe Dashboard, Linear, and Notion. The redesign focuses on clarity, visual hierarchy, quiet borders, and crisp typography while keeping 100% of existing backend APIs, JavaScript functions, event handlers, and DOM `id` attributes intact.

---

## 2. Design Tokens (`css/theme.css`)

### Color Palette (Brand Tokens)
| Token Name | Value | Description |
| :--- | :--- | :--- |
| `--brand-primary` | `#6d0000` | Primary brand accent (MMCOE Deep Red) |
| `--brand-primary-dark` | `#4A0000` | Active/Hover state for primary actions |
| `--brand-primary-light` | `#980000` | Light accent highlight |
| `--brand-crimson` | `#C41E3A` | Crimson highlight token |
| `--bg-canvas` | `#fffaf9` | Page overall canvas background |
| `--surface-white` | `#ffffff` | Primary container & card background |
| `--surface-low` | `#fff1ef` | Subtle highlighted surface background |
| `--text-main` | `#1f1412` | High-contrast main body text |
| `--text-muted` | `#6e5551` | Subdued secondary text and metadata |
| `--border-subtle` | `#ebd6d2` | Card, table, and header quiet border color |
| `--border-focus` | `#d5afaa` | Hover / focus border state |

### Typography & Spacing
- **Font Family**: Inter (`'Inter', sans-serif`) with Material Symbols Outlined icons (`'Material Symbols Outlined'`).
- **Base Font Size**: 12px - 14px for UI elements to maintain high information density.
- **Card Padding**: 16px to 24px (`p-4` to `p-6`).
- **Border Radius**: 6px (`rounded-md`) for controls and inputs, 12px (`rounded-xl`) for main card containers.

---

## 3. Reusable UI Components

### Buttons
- **Primary Action** (`.btn-primary`): Solid primary red background, white text, subtle hover dark red.
- **Secondary Action** (`.btn-secondary`): White background with subtle border (`#ebd6d2`), text in main dark color.
- **Ghost Action** (`.btn-ghost`): Transparent background, quiet hover highlight (`#fff1ef`).
- **Danger Action** (`.btn-danger`): Subdued red background with crimson text for destructive or alert actions.

### Cards (`.ui-card`)
- `bg-white border border-border rounded-xl shadow-sm p-5 transition-all`
- Clean quiet white surface with subtle `#ebd6d2` border and soft drop shadow.

### Inputs & Selects (`.ui-input`)
- `w-full bg-white border border-border rounded-md px-3 py-2 text-xs text-textMain focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary`

### Data Tables (`.ui-table`)
- High density table design with fixed row height, uppercase quiet header styling, subtle bottom borders, and hover background highlighting (`hover:bg-surfaceLow/50`).

### Status Badges (`.chip`)
- Minimal pill shape badge with status dot indicators (Green for Available/Issued, Red for Overdue, Amber for Pending).

---

## 4. Responsive Layout Architecture

### Navigation Layout Strategy
1. **Desktop Sidebar (`lg:flex w-60`)**:
   - Fixed 240px wide navigation panel on the left.
   - Includes MMCOE branding logo, user profile summary, quick navigation tabs (`class="tab-btn"`), and logout action.
2. **Top Navigation Bar (`h-14`)**:
   - Sleek top bar displaying current view breadcrumb, search bar, active user profile pill, and live system status indicators.
3. **Mobile Navigation Bar (`lg:hidden fixed bottom-0`)**:
   - Fixed bottom tab bar for viewports `< 1024px`.
   - Displays 4-5 icon tabs (`class="tab-btn"`) for touch interaction.

### Zero-Break DOM Integration Strategy
- Both desktop sidebar buttons and mobile bottom navigation links share the `.tab-btn` class and `onclick="switchTab('tabName')"` handlers.
- `switchTab()` in `api.js` updates `.active` classes across all elements with `.tab-btn` matching the target tab, ensuring seamless navigation on all screen sizes without modifying a single line of backend or frontend JavaScript.

---

## 5. Preservation & Verification Strategy
- Every `id="..."` attribute across all HTML pages (`login.html`, `dashboard.html`, `librarian-dashboard.html`, `admin-dashboard.html`, `landing.html`) was cataloged prior to redesign and verified post-redesign.
- Endpoint contracts, JSON payload shapes, and event handlers remain 100% original.
