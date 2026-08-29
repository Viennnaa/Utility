package com.viennnaa.utilities.core.settings

/**
 * The settings that apply to the whole app rather than to one mini app.
 *
 * Kept free of Android types so the rules below — which theme wins, what happens
 * to a favourite whose mini app no longer exists — are plain unit tests.
 */

/** How many mini apps can be pinned before the section stops being a shortcut. */
const val MAX_FAVOURITES = 8

/** What the user chose in Settings, which is not the same as what they see. */
enum class ThemeMode {
    /** Follow the device, which is the default and what most people expect. */
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Whether to draw the dark palette.
 *
 * [systemInDark] only matters for [ThemeMode.SYSTEM]; an explicit choice wins
 * over the device, which is the whole point of offering the choice.
 */
fun ThemeMode.resolveDark(systemInDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemInDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

/** Reads a stored mode name, falling back to following the device. */
fun themeModeOf(name: String?): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == name } ?: ThemeMode.SYSTEM

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Material You colours drawn from the wallpaper, on Android 12 and above. */
    val dynamicColor: Boolean = true,
    /** Mini app ids pinned to the top of the home screen, in the order shown. */
    val favouriteIds: List<String> = emptyList(),
)

/**
 * Adds or removes [id] from [favourites].
 *
 * A newly pinned mini app goes to the end, so pinning something does not
 * reshuffle what is already there. Pinning past [MAX_FAVOURITES] is refused
 * rather than silently dropping the oldest: quietly unpinning something the user
 * chose is worse than declining.
 */
fun toggleFavourite(favourites: List<String>, id: String): List<String> = when {
    id in favourites -> favourites - id
    favourites.size >= MAX_FAVOURITES -> favourites
    else -> favourites + id
}

/** True when one more can be pinned. */
fun canPinMore(favourites: List<String>): Boolean = favourites.size < MAX_FAVOURITES

/**
 * Drops ids that no longer name a mini app and any duplicates, keeping order.
 *
 * Stored favourites outlive the catalog: a mini app removed in an update leaves
 * an id behind, and rendering it would mean a tile that opens nothing.
 */
fun sanitizeFavourites(favourites: List<String>, knownIds: Set<String>): List<String> {
    val seen = LinkedHashSet<String>()
    for (id in favourites) {
        if (id in knownIds) seen.add(id)
    }
    return seen.toList().take(MAX_FAVOURITES)
}
