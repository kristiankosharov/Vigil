---
paths:
  - "feature/*/ui/**/*Screen*.kt"
  - "feature/*/ui/**/components/**"
  - "common/ui/**"
  - "**/src/main/res/**"
  - "**/src/androidTest/**"
---

# UI quality: strings and accessibility

## String resources

- Every user-facing string is a resource. No hardcoded text in composables (previews may use
  literals). Domain and data never contain user-facing text; they return `AppError`, and the UI maps
  it with `toUiText()`.
- **Placement:**
  - Feature strings: `:feature:x:ui/src/main/res/values/strings.xml`, prefixed with the feature
    (`users_title`, `users_empty_message`).
  - Shared strings (OK, Cancel, Retry, generic `AppError` messages, common content descriptions):
    `:common:ui/src/main/res/values/strings.xml`, prefixed `common_`.
  - `:app` holds only the app name, launcher labels and the labels of the main navigation tabs
    (`tab_*`, `navigation/MainTabs.kt`).
- R classes are non-transitive: shared strings are `app.vigil.common.ui.R.string.common_retry`.
- `UiText.Resource` for simple strings, `UiText.Plural(@PluralsRes id, count, args)` for quantities.
  Never build sentences by concatenation; use format arguments.
- Translations go in `values-<lang>` of the same module. Strings that must not be translated use
  `translatable="false"`.

## Accessibility

- Every `Icon`/`Image` that carries meaning has a `contentDescription` from string resources;
  decorative ones pass `contentDescription = null`.
- Interactive elements are at least **48×48 dp** (Material components, or
  `Modifier.minimumInteractiveComponentSize()` for custom ones).
- Custom clickable elements set a `role` (`Modifier.clickable(role = Role.Button) { … }`) and, for
  state, `stateDescription` / `toggleableState`.
- List items that read as one unit use `Modifier.semantics(mergeDescendants = true) {}`; screen and
  section titles use `Modifier.semantics { heading() }`.
- Text uses `sp` from the theme typography and never sits in fixed-height containers. Layouts must
  work at 200% font scale.
- Colors come only from `MaterialTheme.colorScheme` (contrast-checked in light and dark). Never convey
  information by color alone.
- Every screen has `@PreviewLightDark` and `@PreviewFontScale` previews.
- Compose UI tests enable the Compose accessibility checks and find nodes by text/content
  description where possible, not by test tags.
