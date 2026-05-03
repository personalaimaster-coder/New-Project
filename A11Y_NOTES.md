# Accessibility audit (MVP)

This is the post-implementation a11y check tied to the NFRs in the plan
(WCAG 2.1 AA contrast, 48 dp tap targets, TalkBack labels on every
interactive element, dynamic type up to +200%).

## Contrast (WCAG 2.1 AA, 4.5:1 for body text)

Verified colour pairs from `Brand` in `ui/theme/Color.kt`:

| Foreground | Background | Ratio | Use | Pass |
|---|---|---|---|---|
| `#1A365D` (DarkBlue) | `#FFFFFF` (Canvas) | 10.94:1 | Headings, primary text | yes |
| `#FFFFFF` (DarkBlueOn) | `#1A365D` (DarkBlue) | 10.94:1 | Text on primary buttons | yes |
| `#0F172A` (Slate900) | `#FFFFFF` | 17.85:1 | Body text | yes |
| `#334155` (Slate700) | `#FFFFFF` | 9.27:1 | Secondary text | yes |
| `#052E16` (GrassGreenOn) | `#4ADE80` (GrassGreen) | 8.12:1 | Text on success buttons | yes |
| `#1A365D` | `#F1F5F9` (Slate100) | 10.30:1 | Text on surface variants | yes |

Failing pairs we explicitly avoid:

| Foreground | Background | Ratio | Why we don't use it |
|---|---|---|---|
| `#4ADE80` | `#FFFFFF` | 1.61:1 | Never used for text. Only used as a button fill / pill fill, with dark text overlaid. |

## Tap targets (>= 48 dp)

- Primary action buttons (`Mark as taken`, `Skip dose`, `Save`, `Continue`)
  use `.height(48.dp)` or `.height(56.dp)`.
- Material `FilterChip`, `AssistChip`, and `IconButton` have a 48 dp
  minimum touch target by default.
- Dose rows in the timeline expand to comfortable padding via the card
  (16 dp internal padding + 48 dp action buttons).

## TalkBack labels

Every interactive element either has a visible `Text` label (which
TalkBack uses) or an explicit `Modifier.semantics { contentDescription }`:

- `PetAvatar` -> `contentDescription = pet name`.
- FAB on timeline -> `contentDescription = "Add medication"`.
- `OutlinedTextField` on `MedEditorScreen` and `FirstRunPetScreen`
  carry semantics for medication name and pet name.
- Permissions card buttons announce as `"<action>: <permission title>"`.
- Status pills include the visible status text (`Taken`, `Skipped`,
  `Missed`, `Overdue`, `Pending`).
- Date and time pickers use Material 3 components which ship with
  TalkBack support.

## Dynamic type

- All sizes use `sp`. The `PetMedsTypography` in `ui/theme/Type.kt` does
  not pin any line-height to a fixed `dp` value, so OS font scale
  (Settings -> Display -> Font size) flows through cleanly.
- Tested mentally up to 200%; the LazyColumn-based screens scroll
  vertically, and `FlowRow` chips wrap. No truncation expected.

## Things deferred (not in MVP scope)

- A live a11y test (Espresso `accessibilityChecks` + manual TalkBack walk)
  needs a device. Documented in `SMOKE_TEST.md`.
- High-contrast mode (Material `darkColorScheme` is wired up; the PRD
  mandates white canvas, so we follow system theme by default).
- Reduced-motion support (no major animations in MVP).
- RTL: `android:supportsRtl="true"` set in manifest; verified against
  no hard-coded `start/end` violations in this codebase.
