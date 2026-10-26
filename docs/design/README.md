# Vigil design system

The source of truth for how Vigil looks: colors, type, shapes, states, components and the screen
flows. The theme and the shared components live in `:common:ui`; this document explains them and
lists the parts of the design that are not in code yet.

- **Design canvas:** <https://claude.ai/artifact/CShYPemjcpaoC5RESdtrPZ> (private; ask the owner
  for access). It holds every screen as an interactive mock-up, grouped by flow, plus component
  state sheets.
- **Code:** `common/ui/src/main/kotlin/app/vigil/common/ui/theme/` (theme) and
  `common/ui/src/main/kotlin/app/vigil/common/ui/designsystem/` (components).

## Principles

1. **Calm and trustworthy.** One blue brand color, lots of neutral surface, color used for meaning
   (status, errors), not decoration.
2. **Security is visible.** Encryption, auto-lock and "PIN or fingerprint to sign" are stated in the
   UI where they matter, in plain words.
3. **Never color alone.** Every status or error pairs color with an icon, a shape or text.
4. **Material 3 first.** Use Material components and their state rules; customise only shape and
   color roles.

## Color

Generated with [Material Theme Builder](https://material-foundation.github.io/material-theme-builder/)
from source color **`#769CDF`** (the third blue wallpaper), tonal-spot scheme. Values are in
`theme/Color.kt`.

`AppTheme` uses this brand scheme by default (`dynamicColor = false`), so the app looks the same on
every device. Screens read colors only from `MaterialTheme.colorScheme`, never hex values
(`.claude/rules/ui-quality.md`).

| Role | Light | Dark | Used for |
|---|---|---|---|
| `primary` / `onPrimary` | `#415F91` / `#FFFFFF` | `#AAC7FF` / `#0A305F` | Main buttons, FAB, links, focused fields, selected radio |
| `primaryContainer` / `onPrimaryContainer` | `#D6E3FF` / `#284777` | `#284777` / `#D6E3FF` | Info banners ("Encrypted on this device"), nav indicator, AI hints, pressed PIN key |
| `secondary` / `secondaryContainer` | `#565F71` / `#DAE2F9` | `#BEC6DC` / `#3E4759` | Reserved |
| `tertiary` / `tertiaryContainer` | `#705575` / `#FAD8FD` | `#DDBCE0` / `#573E5C` | Reserved |
| `error` / `onError` | `#BA1A1A` / `#FFFFFF` | `#FFB4AB` / `#690005` | Error text and borders, destructive actions, unread badge |
| `errorContainer` / `onErrorContainer` | `#FFDAD6` / `#93000A` | `#93000A` / `#FFDAD6` | Error backgrounds |
| `surface` / `onSurface` | `#F9F9FF` / `#191C20` | `#111318` / `#E2E2E9` | Screen background, body text |
| `onSurfaceVariant` | `#44474E` | `#C4C6D0` | Secondary text, icons, section labels |
| `outline` / `outlineVariant` | `#74777F` / `#C4C6D0` | `#8E9099` / `#44474E` | Field and button borders / chip borders, dividers |
| `surfaceContainerLowest` | `#FFFFFF` | `#0C0E13` | Cards, list items, bottom bar |
| `surfaceContainerLow` | `#F3F3FA` | `#191C20` | Bottom sheets |
| `surfaceContainer` | `#EDEDF4` | `#1D2024` | Thumbnails, dialogs, neutral chips |
| `surfaceContainerHigh` | `#E7E8EE` | `#282A2F` | PIN keys (dark) |
| `surfaceContainerHighest` | `#E2E2E9` | `#33353A` | Card borders |
| `inverseSurface` / `inverseOnSurface` | `#2E3036` / `#F0F0F7` | `#E2E2E9` / `#2E3036` | Selected filter chips |

**Light and dark.** Everyday screens follow the system theme. The security screens (create and
confirm PIN, fingerprint setup, unlock, change PIN) and the camera scanner always use the **dark**
scheme (`AppTheme(darkTheme = true)`), so they read as a separate, protected space.

**Status colors (app-specific, not in code yet).** Material has no success or warning roles. Add
these as a theme extension when the documents feature is built, with light and dark values:

| Status | Light | Dark | Shape that goes with it |
|---|---|---|---|
| To sign | `#B54708` | `#FDB022` | Pen icon |
| Signed | `#067647` | `#47CD89` | Check icon |
| Draft | `outline` | `outline` | "…" icon |
| Failed | `error` | `error` | "!" icon |

## Typography

Material 3 type scale (`theme/Type.kt`) with heavier weights: titles and headlines **Bold /
ExtraBold**, labels **Bold**, body **Regular**. Always use `MaterialTheme.typography`; text is in
`sp` and must work at 200% font scale.

The design uses **Manrope**. It is not bundled yet (`TODO(design)` in `Type.kt`): add the font files
to `res/font` and set the family on every style.

| Use | Style |
|---|---|
| Tab title ("Documents") | `headlineMedium`, ExtraBold |
| Screen title in an app bar | `titleMedium` |
| Card and list-item title | `titleSmall` |
| Body, descriptions | `bodyMedium` / `bodySmall` |
| Section label ("SECURITY") | `labelMedium`, uppercase, +0.06em tracking, `onSurfaceVariant` |
| Buttons, chips | `labelLarge` |

## Shape, spacing and size

| Token | Value | Where |
|---|---|---|
| Button corner | 16 dp | All buttons, FAB, cards, list items |
| Field corner | 12 dp | Text fields, banners |
| Chip corner | 18 dp (pill) | Filter and option chips |
| Sheet / dialog corner | 28 dp | Bottom sheets, dialogs |
| Primary button height | 56 dp, full width | One per screen, at the bottom |
| Touch target | ≥ 48 dp | Every interactive element |
| Screen gutter | 20 dp | Left and right page padding |
| Card padding | 14–16 dp | Cards and list items |
| List gap | 10 dp | Between cards |
| Bottom bar | 80 dp, 64×32 dp indicator pill | Main tabs |

## States

Every interactive component supports four states. They follow Material 3, so the stock components
already do most of this.

| State | Rule |
|---|---|
| Enabled | Default colors from the table above. |
| Pressed | 10% state layer of the content color over the container (Material ripple). PIN keys switch to `primaryContainer`. |
| Disabled | Container `onSurface` at 12%, content `onSurface` at 38%. Rows and cards drop to 38% opacity. |
| Error | `error` color **plus** a message or icon. Fields: 2 dp error border + supporting text + error icon. Buttons: `isError = true` switches to `error` / `onError`. |
| Focused (fields) | 2 dp `primary` border, label in `primary`. |

## Components

### In code (`app.vigil.common.ui.designsystem`)

Each component takes `enabled`, `isError` and an optional `interactionSource` (for pressed-state
previews and tests). They take text as parameters; callers pass string resources.

| Component | Use | Error state |
|---|---|---|
| `PrimaryButton` | Main action of a screen ("Approve & sign", "Continue") | Destructive / retry ("Try again") |
| `SecondaryButton` | Outlined secondary action ("Retake") | Error border and text ("Delete") |
| `TertiaryButton` | Low-emphasis action ("Save without signing") | Destructive ("Remove reminder") |
| `AppTextField` | Single-line field: email, phone, title | Error border + `supportingText` |
| `AppFilterChip` | Filters and multi-select options (document filters, "Notify me") | Required group left empty |
| `SwitchRow` | Settings toggle ("Unlock with fingerprint") | Reason it can't be on |
| `CheckboxRow` | Consent before signing | `errorText` under the row |

The PIN pad and PIN dots already exist in `:feature:applock:ui` (`components/PinPad.kt`); keep
them there and match this spec.

### Specified, not in code yet

Build these in the feature that owns them (`:feature:documents:ui`, `:feature:reminders:ui`, `:app`).

- **Document list item.** Card (`surfaceContainerLowest`, 1 dp `surfaceContainerHighest` border,
  16 dp corners) with a 44×56 dp thumbnail, title (`titleSmall`) and meta line (`bodySmall`). No
  text badge: **status is a 22 dp icon in a colored circle on the thumbnail's bottom-right
  corner**, with a 2 dp ring in the card color, and the status name as its content description.
  Pressed: state layer. Disabled: 38% opacity, meta "Processing…". Failed: error border, meta
  "Signing failed · retry" in `error`, still tappable.
- **Bottom navigation.** Three tabs (Documents, Reminders, Account), `surfaceContainerLowest`,
  indicator `primaryContainer` / icon `onPrimaryContainer`. Reminders shows an unread count badge
  in `error`.
- **Scan FAB.** Extended FAB in `primary` with the scan icon, "Scan document". No disabled state.
- **Reminder date block.** 52×56 dp tile with month and day; `#FEF0C7` / `#93370D` when due soon,
  `surfaceContainer` otherwise (move to the status-color extension).
- **Notification preview.** Never shows document contents: "Rental agreement ends in 7 days ·
  Unlock the wallet to view or renew it."

## Screens and flows

Numbers match the board titles on the design canvas.

### 1 · Security: onboarding and unlock (dark)

| # | Screen | Notes |
|---|---|---|
| 1.1 | Create PIN | Step 1 of 3, 6 digits, warns against easy PINs |
| 1.2 | Confirm PIN | Step 2 of 3; mismatch shows an error and shakes the dots |
| 1.3 | Enable fingerprint | Step 3 of 3; "Not now" is allowed |
| 1.4 | Unlock | PIN or fingerprint; "Forgot PIN?" and "Sign in with password" |

### 2 · Main tabs and their subflows

| # | Screen | Opened from |
|---|---|---|
| 2.1 | Documents tab | Bottom bar. Search filters the list in place (no separate screen); filter chips All / To sign / Signed / Drafts; lock → 1.4; scan → 3.1; to-sign item → 3.3 |
| 2.1a | Signed document | Signed item: preview, "Signature valid", signer, date, certificate, fingerprint; Share, Export PDF, Reminder; Delete |
| 2.1b | Draft document | Draft item: "Review not finished", AI details, Continue review → 3.2, Delete draft |
| 2.2 | Reminders tab | Bottom bar. "This week" / "Later", switch per reminder |
| 2.2a | Filter reminders | Filter icon: bottom sheet with Show, Type, Sort by |
| 2.2b | New reminder | FAB: document picker, title, date, "Notify me" chips |
| 2.2c | Reminder details | Item (signing-deadline kind): document link, notify options, Sign now → 3.3, Mark as done |
| 2.3 | Account tab | Bottom bar. Profile, Security, Sign-in methods, Sign out |
| 2.3a | Edit profile | Edit button: photo, name (asks for PIN); email and phone are read-only here |
| 2.3b | Change PIN | Enter current PIN (dark), then continues to 1.1 |
| 2.3c | Auto-lock | Immediately … 15 minutes; 1 minute recommended |
| 2.3d | Email & password | Change email (link confirmation), change password with strength meter |
| 2.3e | Phone number | New number + 6-digit SMS code |
| 2.3f | Sign out | Dialog; optional "Also delete all documents from this device" |

The fingerprint switch and "Require PIN to sign: Always on" have no screens of their own.

### 3 · Scan → Review → Sign

| # | Screen | Notes |
|---|---|---|
| 3.1 | Scan | Camera, edge detection, auto-capture, page counter. No document-type tabs: AI detects the type |
| 3.2 | Review & approve | AI-detected type (changeable), pages, details found; "Reminder added" → 3.2a; Approve & sign / Approve & save |
| 3.2a | Reminder added | Explains the reminder AI created from a date in the document; notify options; notification preview |
| 3.3 | Sign digitally | No hand-drawn signature: signer certificate, SHA-256 fingerprint, key in the device keystore, timestamp; consent checkbox; sign with PIN or fingerprint |

### 4 · Components and states

State sheets for buttons, inputs, security components and lists, plus the status-indicator
options that led to the thumbnail corner icon.

## Security and privacy in the UI

- The wallet locks on start and after the auto-lock delay; signing **always** asks for PIN or
  fingerprint, whatever the auto-lock setting.
- Signing is digital: the key stays in the Android Keystore; the UI shows the document's SHA-256
  fingerprint and the certificate before signing.
- Notifications and recent-apps previews never show document contents.
- Search, AI detection and reminders run on the device; say so where users might wonder.

## Accessibility checklist

- Touch targets ≥ 48 dp; whole rows are the toggle target for switches and checkboxes.
- Every meaningful icon has a content description from string resources; decorative ones pass
  `null`.
- Status and errors always pair color with an icon, shape or text.
- Layouts work at 200% font scale; text never sits in fixed-height containers.
- Every screen has `@PreviewLightDark` and `@PreviewFontScale` previews.
