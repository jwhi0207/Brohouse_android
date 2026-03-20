# BroHouse — Screen Inventory for UI Redesign

## Overview

BroHouse is an Android app (Jetpack Compose + Material3) that helps groups of friends organize a shared house trip. It covers cost splitting, supply coordination, and carpooling. The tech stack is Kotlin + Compose + Firebase Firestore — no web, pure native Android.

**Navigation flow:**
```
Login / Register
    └─ My Trips (list)
           └─ Trip Dashboard (hub)
                  ├─ House Details
                  ├─ Supplies
                  ├─ Carpool  ← (in review, merging soon)
                  └─ Members / Invite
```

**Roles:**
- **Admin (trip owner)** — can edit house details, invite people, add payments for others, delete any ride
- **Member** — can edit their own nights, claim supplies, offer/claim rides

**Color / theme:** Currently uses the default Material3 dynamic color scheme with no custom branding.

---

## Screen 1 — Login

**Route:** `login`

**Purpose:** Entry point. Users sign in with email/password or Google.

**Current layout:**
- Centered column, full screen
- App name "Brohouse" in `displaySmall` typography, primary color
- Subtitle: "Sign in to your account"
- Error card (shown on auth failure, red tinted)
- Email `OutlinedTextField`
- Password `OutlinedTextField` with show/hide toggle icon
- "Sign In" `Button` (full width, shows `CircularProgressIndicator` while loading)
- Divider with "or"
- "Continue with Google" `OutlinedButton` (full width)
- "Don't have an account? Register" `TextButton`

**Notes for redesign:**
- No logo/illustration currently — good opportunity to add branding
- The Google sign-in button has no Google logo/icon
- Loading state only shown inside the Sign In button, not for Google

---

## Screen 2 — Register

**Route:** `register`

**Purpose:** New account creation with email/password or Google.

**Current layout:**
- Mirrors Login screen structure
- Fields: Display Name, Email, Password, Confirm Password
- Password strength/match validation inline
- "Create Account" button
- "Already have an account? Sign In" link

**Notes for redesign:**
- Same structural issues as Login — no branding, no Google icon

---

## Screen 3 — My Trips

**Route:** `trips`

**Purpose:** Lists all trips the user belongs to. Entry point after login.

**Current layout:**
- `TopAppBar`: title "My Trips", sign-out icon button (top right)
- FAB: `+` to create a new trip
- **Empty state:** centered text "No trips yet / Tap + to create your first trip"
- **Trip list:** `LazyColumn` of rows, each showing:
  - Trip name (`titleMedium`)
  - Member count (`bodySmall`, muted)
  - Pending invite badge (if any invites are outstanding)
  - Full-width tap to navigate into the trip
  - `HorizontalDivider` between rows

**Bottom sheet — Create Trip:**
- Single `OutlinedTextField` for trip name
- Cancel / Create buttons

**Notes for redesign:**
- Trip rows are very plain — no thumbnail, date, or visual identity
- No way to distinguish trips at a glance if you have several
- Sign-out is a bare icon with no confirmation flow visible until tapped

---

## Screen 4 — Trip Dashboard

**Route:** `trip/{tripId}`

**Purpose:** The hub for a single trip. Shows summary cards for each feature and the full member cost breakdown.

**Current layout:**
- `TopAppBar`: trip name, back arrow, person-add icon (admin only), overflow menu (sign out)
- `LazyColumn`:
  1. **House Details Card** — thumbnail photo (or placeholder house icon), total nights, total cost, guest count. Taps into House Details screen.
  2. **Supplies Card** — item count + unclaimed count. Taps into Supplies screen.
  3. *(Carpool Card — coming in next merge)*
  4. **Member rows** (or empty state if no members)

**Member row:**
- Avatar (generated from seed, colored circle with initials)
- Name
- Nights stayed chip (moon icon + "N nights" or "Nights TBD")
- Balance chip: "Paid up ✓" (green), "$X.XX Due" (red), or "Set nights to calculate" (muted)
- Paid chip (shown when partial payment recorded): "$X.XX Paid" (green)
- `⋮` overflow menu → "Edit Nights" or "Add Payment" (admin/self gated)

**Bottom sheets (triggered from member row):**
- **Edit Nights Sheet** — number input, validates against trip total nights
- **Add Payment Sheet** — shows cost breakdown summary (owed / paid / remaining), dollar input

**Notes for redesign:**
- The dashboard feels like a plain list — no visual hierarchy between the feature cards and the member section
- Member rows are data-dense but lack visual breathing room
- The avatar is a simple colored circle — no photo support
- The House Details card photo area is a good anchor but feels generic

---

## Screen 5 — House Details

**Route:** `trip/{tripId}/house_details`

**Purpose:** View or edit the rental house info — URL, nights, and total cost.

**Current layout:**
- `TopAppBar`: "House Details", back arrow, "Save" text button (admin only, disabled until valid)
- Scrollable `Column`:
  - Read-only notice (non-admins)
  - **House URL** — labeled text field, paste icon, "Open in Browser" link button (when URL is set)
  - **Total Nights** — number field
  - **Total Cost** — dollar-prefixed decimal field

**Notes for redesign:**
- Very form-like and utilitarian — no preview of the house
- The URL → thumbnail fetch happens on save (async), but there's no visual preview before saving
- Admins and non-admins see the same layout; non-admins just get read-only fields

---

## Screen 6 — Supplies

**Route:** `trip/{tripId}/supplies`

**Purpose:** Collaborative supply list — members add items, claim what they're bringing, and track quantities.

**Current layout:**
- `TopAppBar`: "Supplies", back arrow
- FAB: `+` to add a supply item
- **Quick Add section** (collapsible): horizontal chips of preset items (Burgers, Hot Dogs, Weed, etc.) not yet on the list — tap to add and immediately claim
- **Category sections** (Food, Disposables, Entertainment, Drugs & Paraphernalia, Other):
  - Section header with collapse toggle and claimed progress ("X/Y claimed")
  - Items within each category, drag-to-reorder (long press handle)
  - Each item row:
    - Item name
    - Quantity / claimed-by info
    - Swipe right → delete (red background with trash icon)
    - Tap → Claim dialog

**Dialogs:**
- **Claim dialog** — select member from list, enter quantity with wheel picker (number + unit) or free text
- **Manage Claims dialog** — see who claimed what, remove claims, add more
- **Add Supply Item sheet** — name, category chips, quantity picker

**Notes for redesign:**
- The category headers are functional but plain
- The wheel number picker is custom-built and works well — worth keeping the UX, just restyling
- The quick-add chips are a nice pattern but visually crammed
- Swipe-to-delete is a useful interaction but not discoverable

---

## Screen 7 — Members / Invite

**Route:** `trip/{tripId}/invite`

**Purpose:** View current members and (admin only) invite new people by email.

**Current layout:**
- `TopAppBar`: "Members", back arrow
- `LazyColumn`:
  - **Invite by Email** (admin only): email text field + "Invite" button side by side
  - **Pending Invites** (admin only): list of email strings, each with an ✕ cancel button
  - **Current Members**: avatar + display name + email for each member

**Notes for redesign:**
- Admin and non-admin see very different amounts of content with no visual distinction
- Members are identified by email — no avatar prominence
- No indication of who the trip owner/admin is

---

## Screen 8 — Carpool *(merging soon from `ben/carpool`)*

**Route:** `trip/{tripId}/carpool`

**Purpose:** Coordinate rides to the house — members offer vehicles, others claim seats, and people can flag they need a lift.

**Current layout:**
- `TopAppBar`: "Carpool", back arrow
- FAB: "+" to offer a ride (hidden if user already has a vehicle)
- `LazyColumn`:
  - **Vehicles section header**
  - **Empty state** if no rides yet
  - **Ride cards** — one per vehicle:
    - Vehicle emoji + label + driver name
    - Departure location, departure time, return time
    - Seat availability ("X of Y seats open" / "Full")
    - Passenger names
    - Notes (if any)
    - Edit icon + Delete icon (driver/owner only)
    - "Claim Seat" / "Leave Ride" / "Full" button
  - **Need a Ride section header**
  - "I Need a Ride" button (greyed out with tooltip if user has a vehicle)
  - List of members who flagged they need a lift

**Bottom sheet — Add/Edit Vehicle:**
- Vehicle emoji picker (5 options: 🚗 🚐 🛻 🚌 🏎️)
- Vehicle description text field
- Departure location text field
- Seat counter (1–8, +/− buttons)
- Departure date + time (date picker → time picker flow)
- Return date + time (same flow, calendar pre-scrolled to departure date)
- Notes text field

**Notes for redesign:**
- Ride cards are content-heavy — could benefit from a more visual treatment
- The emoji picker chips are functional but could be styled more distinctively
- The two-section layout (Vehicles / Need a Ride) needs clear visual separation

---

## Bottom Sheets (shared, used across screens)

These appear on top of various screens:

| Sheet | Triggered from | Fields |
|---|---|---|
| Create Trip | My Trips FAB | Trip name |
| Edit Nights | Trip Dashboard member row | Nights number input |
| Add Payment | Trip Dashboard member row | Cost summary, payment amount |
| Add Supply Item | Supplies FAB | Name, category, quantity picker |
| Add / Edit Vehicle | Carpool FAB or edit icon | Emoji, label, location, seats, dates, notes |

---

## AvatarView (shared component)

Used on Trip Dashboard member rows and Members screen. Currently a colored circle with initials, color derived from a random seed stored in Firestore. No photo upload — purely generated.
