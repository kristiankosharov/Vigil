package app.vigil.feature.settings.domain.model

/** How the app picks light or dark. The security screens are always dark, whatever this says. */
public enum class ThemeMode {
    /** Follow the device's dark theme setting. */
    System,
    Light,
    Dark,
}
